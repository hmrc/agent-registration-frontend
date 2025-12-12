/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.amls

import com.softwaremill.quicklens.*
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsName
import uk.gov.hmrc.agentregistration.shared.upscan.ObjectStoreUrl
import uk.gov.hmrc.agentregistration.shared.upscan.UploadDetails
import uk.gov.hmrc.agentregistration.shared.upscan.UploadId
import uk.gov.hmrc.agentregistration.shared.upscan.UploadIdGenerator
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.UpscanInitiateConnector
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.amls.AmlsEvidenceUploadPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.amls.UpscanErrorPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.amls.AmlsEvidenceUploadProgressPage
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.ObjectStoreService
import uk.gov.hmrc.agentregistrationfrontend.services.UpscanProgressService
import uk.gov.hmrc.objectstore.client.Path

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AmlsEvidenceUploadController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: AmlsEvidenceUploadPage,
  progressView: AmlsEvidenceUploadProgressPage,
  errorView: UpscanErrorPage,
  appConfig: AppConfig,
  upscanInitiateConnector: UpscanInitiateConnector,
  applicationService: AgentApplicationService,
  amlsCodes: AmlsCodes,
  upscanProgressService: UpscanProgressService,
  objectStoreService: ObjectStoreService,
  uploadIdGenerator: UploadIdGenerator
)(using ec: ExecutionContext)
extends FrontendController(mcc, actions):

  val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      _.agentApplication.amlsDetails.exists(!_.isHmrc),
      implicit r =>
        logger.warn("Uploaded evidence is not required as supervisor is HMRC, redirecting to Check Your Answers")
        Redirect(routes.CheckYourAnswersController.show.url)
    )
    .ensure(
      _.agentApplication.getAmlsDetails.amlsExpiryDate.isDefined, // safe to getAmlsDetails as ensured above
      implicit r =>
        logger.warn("Missing AmlsExpiryDate, redirecting to AmlsExpiryDate page")
        Redirect(routes.AmlsExpiryDateController.show.url)
    )

  def show: Action[AnyContent] = baseAction.async:
    implicit request =>
      val amlsCode: AmlsCode = request.agentApplication.getAmlsDetails.supervisoryBody
      val amlsName: AmlsName = amlsCodes.getSupervisoryName(amlsCode)
      for
        upscanInitiateResponse <- upscanInitiateConnector.initiate(
          redirectOnSuccessUrl = uri"${appConfig.thisFrontendBaseUrl + routes.AmlsEvidenceUploadController.showUploadResult.url}",
          // cannot use controller.routes for the error url because upscan will respond with query parameters
          redirectOnErrorUrl = appConfig.thisFrontendBaseUrl + "/agent-registration/apply/anti-money-laundering/evidence/error",
          maxFileSize = appConfig.Upscan.maxFileSize
        )
        uploadDetails = UploadDetails(
          uploadId = uploadIdGenerator.nextUploadId(),
          status = UploadStatus.InProgress,
          reference = upscanInitiateResponse.reference
        )
        // store the upscan fileReference and new uploadId in the application
        // and initiate the upscan progress tracking in agent-registration backend
        _ <- applicationService
          .upsert(
            request.agentApplication.asLlpApplication
              .modify(_.amlsDetails.each.amlsEvidence)
              .setTo(Some(uploadDetails))
          ).flatMap: _ =>
            upscanProgressService
              .initiate(uploadDetails)
      yield Ok(view(
        upscanInitiateResponse = upscanInitiateResponse,
        supervisoryBodyName = amlsName
      ))

  /** Handles file upload errors from Upscan.
    *
    * This endpoint is called when a file transfer to Upscan service fails. It is not the endpoint for reporting file scanning failures, that happens in
    * showResult which reads the upload status from the application. Upscan will redirect to this endpoint and append error information as query parameters to
    * the redirect URL.
    */
  def showError(
    errorCode: String,
    errorMessage: String,
    errorRequestId: String,
    key: String
  ): Action[AnyContent] = actions
    .Applicant
    .getApplicationInProgress:
      implicit request =>
        logger.warn(
          s"Received Upscan upload error callback: errorCode=$errorCode, errorMessage=$errorMessage, " +
            s"errorRequestId=$errorRequestId, key=$key"
        )
        Ok(errorView(errorCode))

  def showUploadResult: Action[AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .async:
      implicit request =>
        upscanProgressService.getUpscanStatus().flatMap:
          case Some(status) if status != UploadStatus.InProgress =>
            for {
              objectStorePath <- objectStoreService
                .transferFileToObjectStore(
                  request
                    .agentApplication
                    .getAmlsDetails
                    .getAmlsEvidence
                    .reference,
                  status
                )
              updatedApplication =
                (objectStorePath, status) match
                  case (Some(path: Path.File), success: UploadStatus.UploadedSuccessfully) =>
                    request.agentApplication.asLlpApplication
                      .modify(_.amlsDetails.each.amlsEvidence.each.status)
                      .setTo(
                        success
                          .modify(_.objectStoreLocation)
                          .setTo(Some(ObjectStoreUrl(path.asUri)))
                      )
                  case _ =>
                    request.agentApplication.asLlpApplication
                      .modify(_.amlsDetails.each.amlsEvidence.each.status)
                      .setTo(status)
              _ <- applicationService.upsert(updatedApplication)
            } yield Ok(progressView(status))
          case _ =>
            // no status change, just render the page
            Future.successful(Ok(progressView(UploadStatus.InProgress)))

  private val corsHeaders: Seq[(String, String)] = Seq(
    "Access-Control-Allow-Origin" -> appConfig.thisFrontendBaseUrl,
    "Access-Control-Allow-Credentials" -> "true",
    "Access-Control-Allow-Methods" -> "GET, OPTIONS"
  )

  /** This endpoint is called via JavaScript in a poll loop to check the status of the file upload. The upload status is encoded in the HTTP status response:
    */
  def checkUploadStatus: Action[AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .async:
      implicit request =>
        upscanProgressService.getUpscanStatus().map:
          case Some(status) =>
            status match
              case UploadStatus.InProgress => NoContent.withHeaders(corsHeaders*)
              case _: UploadStatus.UploadedSuccessfully => Accepted.withHeaders(corsHeaders*)
              case UploadStatus.Failed => BadRequest.withHeaders(corsHeaders*)
          case _ => NoContent.withHeaders(corsHeaders*) // no status change
