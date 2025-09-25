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

package uk.gov.hmrc.agentregistrationfrontend.controllers.amls

import com.softwaremill.quicklens.*
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsName
import uk.gov.hmrc.agentregistration.shared.upscan.UploadDetails
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.UpscanConnector
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.ErrorTemplate
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.amls.AmlsEvidenceUploadPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.amls.AmlsEvidenceUploadProgressPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class AmlsEvidenceUploadController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  view: AmlsEvidenceUploadPage,
  progressView: AmlsEvidenceUploadProgressPage,
  errorView: ErrorTemplate,
  appConfig: AppConfig,
  upscanInitiateConnector: UpscanConnector,
  applicationService: ApplicationService,
  amlsCodes: AmlsCodes
)(implicit ec: ExecutionContext)
extends FrontendController(mcc)
with I18nSupport:

  def show: Action[AnyContent] = actions.getApplicationInProgress.async:
    implicit request =>
      val amlsCode: AmlsCode = request.agentApplication.getAmlsDetails.supervisoryBody
      val amlsName: AmlsName = amlsCodes.getSupervisoryName(amlsCode)

      for
        upscanInitiateResponse <- upscanInitiateConnector.initiate(
          redirectOnSuccess = Some(appConfig.upscanRedirectBase + routes.AmlsEvidenceUploadController.showResult.url),
          // cannot use controller.routes for the error url because upscan will respond with query parameters
          redirectOnError = Some(appConfig.upscanRedirectBase + "/agent-registration/apply/anti-money-laundering/evidence/error"),
          maxFileSize = appConfig.maxFileSize
        )
        // store the upscan fileReference in the application
        _ <- applicationService
          .upsert(
            request.agentApplication
              .modify(_.amlsDetails.each.amlsEvidence)
              .setTo(Some(UploadDetails(
                status = UploadStatus.InProgress,
                reference = upscanInitiateResponse.fileReference
              )))
          )
      yield Ok(view(
        upscanInitiateResponse = upscanInitiateResponse,
        supervisoryBodyName = amlsName
      ))

  /** Handles file upload errors from Upscan.
    *
    * This endpoint is called when a file transfer to Upscan service fails. Upscan will redirect to this endpoint and append error information as query
    * parameters to the redirect URL.
    */
  def showError(
    errorCode: String,
    errorMessage: String,
    errorRequestId: String,
    key: String
  ): Action[AnyContent] = actions.getApplicationInProgress:
    implicit request =>
      Ok(errorView(
        pageTitle = "Upload Error",
        heading = "Upload Error",
        message = s"$errorMessage, Code: $errorCode, RequestId: $errorRequestId, FileReference: $key"
      ))

  def showResult: Action[AnyContent] = actions.getApplicationInProgress:
    implicit request =>
      Ok(progressView(request
        .agentApplication
        .getAmlsDetails
        .getAmlsEvidence
        .status))

  private val corsHeaders: Seq[(String, String)] = Seq(
    "Access-Control-Allow-Origin" -> appConfig.allowedCorsOrigin,
    "Access-Control-Allow-Credentials" -> "true",
    "Access-Control-Allow-Methods" -> "GET, OPTIONS"
  )

  // this method returns a status code only for AJAX polling
  def pollResultWithJavaScript: Action[AnyContent] = actions.getApplicationInProgress:
    implicit request =>
      request
        .agentApplication
        .getAmlsDetails
        .getAmlsEvidence
        .status match {
        case UploadStatus.InProgress => NoContent.withHeaders(corsHeaders*)
        case _: UploadStatus.UploadedSuccessfully => Accepted.withHeaders(corsHeaders*)
        case failed: UploadStatus.Failed if failed.failureReason == "QUARANTINE" => Conflict.withHeaders(corsHeaders*)
        case _: UploadStatus.Failed => BadRequest.withHeaders(corsHeaders*)
      }
