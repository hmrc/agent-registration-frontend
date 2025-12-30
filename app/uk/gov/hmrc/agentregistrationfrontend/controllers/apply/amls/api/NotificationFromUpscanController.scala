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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.amls.api

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.*
import uk.gov.hmrc.agentregistrationfrontend.repository.UploadRepo
import uk.gov.hmrc.agentregistrationfrontend.util.Errors

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class NotificationFromUpscanController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  uploadRepo: UploadRepo
)(using ec: ExecutionContext)
extends FrontendController(mcc, actions):

  /** Handles callbacks from the upscan-notify service with file upload notifications.
    *
    * For detailed callback format see [[UploadNotificationRequest]]
    */
  def processNotificationFromUpscan(uploadId: UploadId): Action[UploadNotificationRequest] =
    actions
      .action
      .async(parse.json[UploadNotificationRequest]):
        implicit request =>
          val notificationRequest: UploadNotificationRequest = request.body
          logger.info(s"Upscan notification received for uploadId: $uploadId, status: $notificationRequest")

          uploadRepo.findById(uploadId).flatMap:
            case None =>
              val message = s"No upload record found for uploadId: $uploadId."
              logger.error(message)
              Future.successful(NotFound(message))
            case Some(upload: Upload) =>
              val uploadStatus: UploadStatus = request.body.asUploadStatus
              for
                _ <- Errors.requireF(
                  request.body.reference === upload.fileUploadReference,
                  s"Callback reference '${request.body.reference.value}'does not match upload reference '${upload.fileUploadReference.value}'"
                )
                _ <- uploadRepo.upsert(upload.copy(uploadStatus = uploadStatus))
              yield Ok("")

  extension (notificationRequest: UploadNotificationRequest)
    def asUploadStatus: UploadStatus =
      notificationRequest match
        case unr: UploadNotificationRequest.Success =>
          UploadStatus.UploadedSuccessfully(
            fileName = unr.uploadDetails.fileName,
            mimeType = unr.uploadDetails.fileMimeType,
            downloadUrl = unr.downloadUrl,
            sizeInBytes = unr.uploadDetails.size,
            checksum = unr.uploadDetails.checksum
          )
        case f: UploadNotificationRequest.Failed =>
          UploadStatus.Failed(
            failureReason = f.failureDetails.failureReason,
            messageFromUpscan = f.failureDetails.message
          )
