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

import play.api.libs.json.Json
import play.api.mvc.*
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.FileUploadReference
import uk.gov.hmrc.agentregistrationfrontend.services.ObjectStoreService
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.AuthProviders
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.auth.core.Enrolment

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EvidenceDownloadUrlController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  objectStoreService: ObjectStoreService,
  appConfig: AppConfig,
  af: AuthorisedFunctions
)(using ec: ExecutionContext)
extends FrontendController(mcc, actions):

  // Returns a signed upscan download URL for the given fileReference
  def evidenceDownloadUrl(fileReference: FileUploadReference): Action[AnyContent] = Action.async:
    implicit request =>
      af.authorised((Enrolment(appConfig.Stride.strideRoleAmls) or Enrolment(appConfig.Stride.strideRoleSmu)) and AuthProviders(PrivilegedApplication)).apply:
        objectStoreService.getEvidenceDownloadUrl(fileReference).map {
          case Some(url) => Ok(Json.obj("downloadUrl" -> url.downloadUrl.toString))
          case None => NotFound
        }
      .recoverWith:
        case _: AuthorisationException => Future.successful(Forbidden)
