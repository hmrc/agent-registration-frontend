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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.amls.api

import play.api.http.HeaderNames
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.upload.FileUploadReference
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.ObjectStoreService
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.auth.core.AuthProviders
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.time.ZoneOffset.UTC
import javax.inject.Singleton
import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

@Singleton
class EvidenceDownloadController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  objectStoreService: ObjectStoreService,
  appConfig: AppConfig,
  af: AuthorisedFunctions
)(using ec: ExecutionContext)
extends FrontendController(mcc, actions)
with RequestAwareLogging:

  def evidenceDownload(fileReference: FileUploadReference): Action[AnyContent] = Action.async:
    implicit request =>
      given HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
      af.authorised(Enrolment(appConfig.Stride.strideRoleAmls) or Enrolment(appConfig.Stride.strideRoleSmu) and AuthProviders(PrivilegedApplication))
        .apply:
          objectStoreService.getEvidenceFile(fileReference).map:
            case None => NotFound
            case Some(file) =>
              Ok.streamed(
                content = file.content,
                contentLength = Some(file.metadata.contentLength),
                contentType = Some(file.metadata.contentType)
              )
                .withHeaders(HeaderNames.CONTENT_MD5 -> file.metadata.contentMd5.value)
                .withDateHeaders(HeaderNames.LAST_MODIFIED -> file.metadata.lastModified.atZone(UTC))
        .recoverWith:
          case e: AuthorisationException =>
            logger.info(s"Unauthorised because of '${e.reason}', $e", e)
            Future.successful(Forbidden)
