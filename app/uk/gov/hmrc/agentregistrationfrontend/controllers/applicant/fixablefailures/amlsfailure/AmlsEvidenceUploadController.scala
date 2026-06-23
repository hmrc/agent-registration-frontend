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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures.amlsfailure

import com.softwaremill.quicklens.*
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.amls.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.amls.AmlsSupervisoryBodyCode
import uk.gov.hmrc.agentregistration.shared.amls.AmlsName
import uk.gov.hmrc.agentregistration.shared.amls.AmlsEvidence
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix._3.AmlsFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.upload.UploadId
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.AgentRegistrationConnector
import uk.gov.hmrc.agentregistrationfrontend.connectors.UpscanInitiateConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.Upload
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.UploadIdGenerator
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.UploadStatus
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.UpscanErrorCode
import uk.gov.hmrc.agentregistrationfrontend.repository.UploadRepo
import uk.gov.hmrc.agentregistrationfrontend.services.ObjectStoreService
import uk.gov.hmrc.agentregistrationfrontend.services.UpscanInitiateService
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.amls.AmlsEvidenceUploadPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.amls.AmlsEvidenceUploadProgressPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.amls.UpscanErrorPage

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AmlsEvidenceUploadController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
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

  val baseAction: ActionBuilderWithData[DataWithApplicationAndBpr] = actions
    .getApplicationAfterSentForRisking
    .behindFeatureFlag(appConfig.Features.fixableFailures)
    .ensure(
      condition = !_.agentApplication.getFixableAmlsDetails.isHmrc,
      resultWhenConditionNotMet =
        implicit r =>
          logger.warn("Uploaded evidence is not required as supervisor is HMRC, redirecting to Check Your Answers")
          Redirect(AppRoutes.fixablefailures.amlsfailure.CheckYourAnswersController.show.url)
    )
    .ensure(
      condition = _.agentApplication.getFixableAmlsDetails.amlsRegistrationNumber.isDefined,
      implicit r =>
        logger.warn("Missing AmlsRegistrationNumber, redirecting to AmlsRegistrationNumber page")
        Redirect(AppRoutes.fixablefailures.amlsfailure.AmlsRegistrationNumberController.show.url)
    )

  def show: Action[AnyContent] = baseAction.async:
    implicit request =>
      val amlsCode: AmlsSupervisoryBodyCode = request.agentApplication.getFixableAmlsDetails.supervisoryBody
      val amlsName: AmlsName = amlsCodes.getSupervisoryName(amlsCode)
      val uploadId: UploadId = uploadIdGenerator.nextUploadId()
      for
        upscanInitiateResponse: UpscanInitiateConnector.UpscanInitiateResponse <- upscanInitiateService.initiate(
          uploadId = uploadId,
          isFix = true
        )
        upload = Upload(
          _id = uploadId,
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
      implicit request =>
        for
          upload: Upload <- uploadRepo.findLatestByInternalUserId(request.internalUserId).map(_.getOrThrowExpectedDataMissing("upload"))
          _ <- updateRecordsIfNeeded(upload, request.agentApplication)
        yield Ok(progressView(upload.uploadStatus))

  private def updateRecordsIfNeeded(
    upload: Upload,
    agentApplication: AgentApplication
  )(using request: RequestHeader): Future[Unit] =
    upload.uploadStatus match
      case UploadStatus.InProgress => Future.successful(())
      case _: UploadStatus.Failed => Future.successful(())
      case succeeded: UploadStatus.UploadedSuccessfully =>
        for
          _ <-
            agentApplication.getFixableAmlsDetails.amlsEvidence.fold(Future.successful(())) {
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
          updatedAmlsDetails: AmlsDetails = agentApplication.getFixableAmlsDetails
            .modify(_.amlsEvidence)
            .setTo(
              Some(AmlsEvidence(
                fileUploadReference = upload.fileUploadReference,
                fileName = succeeded.fileName,
                objectStoreLocation = objectStoreLocation
              ))
            )
          updatedFixes: Seq[EntityFix] =
            agentApplication
              .getRiskingOutcomeEntity match
              case f: RiskingOutcomeEntity.FailedFixable =>
                f.fixes.map:
                  case a: AmlsFix => a.modify(_.amlsDetails).setTo(Some(updatedAmlsDetails))
                  case other: EntityFix => other
              case _ => throw new IllegalStateException("Risking outcome is not fixable. Cannot submit Amls registration number.")
          _ <- agentRegistrationConnector.upsertApplication(
            agentApplication
              .modify(_.riskingOutcomeEntity.each)
              .using:
                case f: RiskingOutcomeEntity.FailedFixable => f.copy(fixes = updatedFixes)
                case other => other
          )
        yield ()

  /** This endpoint is called via JavaScript in a poll loop to check the status of the file upload. The upload status is encoded in the HTTP status response:
    */
  def checkUploadStatusJs: Action[AnyContent] = actions
    .getApplicationAfterSentForRisking
    .async:
      implicit request =>

        extension (r: Result)
          private def withCorsHeaders: Result = r.withHeaders(
            "Access-Control-Allow-Origin" -> appConfig.thisFrontendBaseUrl,
            "Access-Control-Allow-Credentials" -> "true",
            "Access-Control-Allow-Methods" -> "GET, OPTIONS"
          )

        uploadRepo.findLatestByInternalUserId(request.internalUserId).map:
          case Some(status) =>
            status.uploadStatus match
              case UploadStatus.InProgress => NoContent.withCorsHeaders
              case _: UploadStatus.UploadedSuccessfully => Accepted.withCorsHeaders
              case failure: UploadStatus.Failed if failure.isInQuarantine => Conflict.withCorsHeaders
              case _: UploadStatus.Failed => BadRequest.withCorsHeaders // generic error as reason for file rejection is not known here, we do know it's not file type or file size as we are using JS validation - file could be corrupted for example
          case None =>
            Errors.throwServerErrorException(
              s"Upload record not found in database but expected to exist"
            )
