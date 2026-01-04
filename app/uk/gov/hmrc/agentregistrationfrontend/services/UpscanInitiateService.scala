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

package uk.gov.hmrc.agentregistrationfrontend.services

import play.api.mvc.RequestHeader
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistration.shared.upload.UploadId
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.UpscanInitiateConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class UpscanInitiateService @Inject() (
  upscanInitiateConnector: UpscanInitiateConnector,
  appConfig: AppConfig
)
extends RequestAwareLogging:

  def initiate(uploadId: UploadId)(using RequestHeader): Future[UpscanInitiateConnector.UpscanInitiateResponse] = upscanInitiateConnector.initiate(
    redirectOnSuccessUrl = uri"${appConfig.thisFrontendBaseUrl + AppRoutes.apply.amls.AmlsEvidenceUploadController.showUploadResult.url}",
    redirectOnErrorUrl =
      uri"${appConfig.thisFrontendBaseUrl + AppRoutes.apply.amls.AmlsEvidenceUploadController.showError(
          errorCode = None,
          errorMessage = None,
          errorRequestId = None,
          key = None
        ).url}",
    callbackUrl = uri"${appConfig.thisFrontendBaseUrl + AppRoutes.apply.amls.api.NotificationFromUpscanController.processNotificationFromUpscan(uploadId).url}",
    maxFileSize = appConfig.Upscan.maxFileSize
  )
