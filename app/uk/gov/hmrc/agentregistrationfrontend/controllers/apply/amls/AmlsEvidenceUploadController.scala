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
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsName
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.AgentRegistrationConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.Upload
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.UploadIdGenerator
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.UploadStatus
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.UpscanErrorCode
import uk.gov.hmrc.agentregistrationfrontend.repository.UploadRepo
import uk.gov.hmrc.agentregistrationfrontend.services.ObjectStoreService
import uk.gov.hmrc.agentregistrationfrontend.services.UpscanInitiateService
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.amls.AmlsEvidenceUploadPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.amls.AmlsEvidenceUploadProgressPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.amls.UpscanErrorPage

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AmlsEvidenceUploadController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  amlsEvidenceUploadPage: AmlsEvidenceUploadPage,
  progressView: AmlsEvidenceUploadProgressPage,
  upscanErrorPage: UpscanErrorPage,
  appConfig: AppConfig,
  upscanInitiateService: UpscanInitiateService,
  amlsCodes: AmlsCodes,
  objectStoreService: ObjectStoreService,
  uploadIdGenerator: UploadIdGenerator,
  agentRegistrationConnector: AgentRegistrationConnector,
  uploadRepo: UploadRepo,
  clock: Clock
)(using ec: ExecutionContext)
extends FrontendController(mcc, actions):

  val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      _.agentApplication.amlsDetails.exists(!_.isHmrc),
      implicit r =>
        logger.warn("Uploaded evidence is not required as supervisor is HMRC, redirecting to Check Your Answers")
        Redirect(AppRoutes.apply.amls.CheckYourAnswersController.show.url)
    )
    .ensure(
      _.agentApplication.getAmlsDetails.amlsExpiryDate.isDefined, // safe to getAmlsDetails as ensured above
      implicit r =>
        logger.warn("Missing AmlsExpiryDate, redirecting to AmlsExpiryDate page")
        Redirect(AppRoutes.apply.amls.AmlsExpiryDateController.show.url)
    )

  def showAmlsEvidenceUploadPage: Action[AnyContent] = baseAction.async:
    implicit request =>
      val amlsCode: AmlsCode = request.agentApplication.getAmlsDetails.supervisoryBody
      val amlsName: AmlsName = amlsCodes.getSupervisoryName(amlsCode)
      for
        upscanInitiateResponse <- upscanInitiateService.initiate(uploadIdGenerator.nextUploadId())
        upload = Upload(
          _id = uploadIdGenerator.nextUploadId(),
          internalUserId = request.internalUserId,
          createdAt = Instant.now(clock),
          uploadStatus = UploadStatus.InProgress,
          fileUploadReference = upscanInitiateResponse.reference
        )
        _ <- uploadRepo.upsert(upload) // generate ephemeral record to track upload status
      yield Ok(amlsEvidenceUploadPage(
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
    errorCode: Option[String],
    errorMessage: Option[String],
    errorRequestId: Option[String],
    key: Option[String]
  ): Action[AnyContent] = baseAction:
    implicit request =>
      logger.warn(
        s"Received Upscan upload error callback: errorCode=$errorCode, errorMessage=$errorMessage, errorRequestId=$errorRequestId, key=$key"
      )
      val upscanErrorCode: UpscanErrorCode =
        (for
          errorCodeString <- errorCode
          upscanErrorCode <- UpscanErrorCode.values.find(_.toString.toLowerCase === errorCodeString.toLowerCase)
        yield upscanErrorCode).getOrElse(UpscanErrorCode.Unknown)
      Ok(upscanErrorPage(upscanErrorCode))

  def showUploadResult: Action[AnyContent] = baseAction
    .async:
      implicit request: AgentApplicationRequest[AnyContent] =>
        for
          upload: Upload <- uploadRepo.findLatestByInternalUserId(request.internalUserId).map(_.getOrThrowExpectedDataMissing("upload"))
          _ <- updateRecordsIfNeeded(upload, request.agentApplication.asLlpApplication)
        yield Ok(progressView(upload.uploadStatus))

  private def updateRecordsIfNeeded(
    upload: Upload,
    agentApplication: AgentApplicationLlp
  )(using request: RequestHeader): Future[Unit] =
    upload.uploadStatus match
      case UploadStatus.InProgress => Future.successful(())
      case _: UploadStatus.Failed => Future.successful(())
      case succeeded: UploadStatus.UploadedSuccessfully =>
        if agentApplication.getAmlsDetails.amlsEvidence.exists(_.uploadId === upload.uploadId)
        then Future.successful(()) // Evidence already exists for this upload - skipping duplicate transfer to Object Store and database update
        else
          for
            _ <-
              agentApplication.getAmlsDetails.amlsEvidence.fold(Future.successful(())) {
                amlsEvidence =>
                  logger.info(s"Deleting stale evidence from ObjectStore: ${amlsEvidence.objectStoreLocation} (evidence replaced by new upload)")
                  objectStoreService.deleteObject(amlsEvidence.objectStoreLocation)
              }
            objectStoreLocation <-
              logger.info(s"Transferring AmlsEvidence to ObjectStore: ${succeeded.fileName}")
              objectStoreService.transferFileToObjectStore(
                fileReference = upload.fileUploadReference,
                downloadUrl = succeeded.downloadUrl,
                mimeType = succeeded.mimeType,
                checksum = succeeded.checksum,
                fileName = succeeded.fileName
              )
            _ <- agentRegistrationConnector.upsertApplication(
              agentApplication.asLlpApplication
                .modify(_.amlsDetails.each.amlsEvidence)
                .setTo(
                  Some(AmlsEvidence(
                    uploadId = upload.uploadId,
                    fileName = succeeded.fileName,
                    objectStoreLocation = objectStoreLocation
                  ))
                )
            )
          yield ()

  /** This endpoint is called via JavaScript in a poll loop to check the status of the file upload. The upload status is encoded in the HTTP status response:
    */
  def checkUploadStatusJs: Action[AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .async:
      implicit request: AgentApplicationRequest[AnyContent] =>

        extension (r: Result)
          def withCorsHeaders: Result = r.withHeaders(
            "Access-Control-Allow-Origin" -> appConfig.thisFrontendBaseUrl,
            "Access-Control-Allow-Credentials" -> "true",
            "Access-Control-Allow-Methods" -> "GET, OPTIONS"
          )

        uploadRepo.findLatestByInternalUserId(request.internalUserId).map:
          case Some(status) =>
            status.uploadStatus match
              case UploadStatus.InProgress => NoContent.withCorsHeaders
              case _: UploadStatus.UploadedSuccessfully => Accepted.withCorsHeaders
              case _: UploadStatus.Failed => BadRequest.withCorsHeaders
          case None =>
            Errors.throwServerErrorException(
              s"Upload record not found in database but expected to exist"
            )
