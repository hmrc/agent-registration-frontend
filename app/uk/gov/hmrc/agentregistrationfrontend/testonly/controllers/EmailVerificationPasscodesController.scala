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

package uk.gov.hmrc.agentregistrationfrontend.testonly.controllers

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.shared.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.testonly.controllers.EmailVerificationPasscodesController.*
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.EmailVerificationPasscodesPage
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.isSignedIn
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

object EmailVerificationPasscodesController:

  final case class Passcodes(passcodes: List[Passcode])

  object Passcodes:
    given OFormat[Passcodes] = Json.format[Passcodes]

  final case class Passcode(
    email: String,
    passcode: String
  )

  object Passcode:
    given OFormat[Passcode] = Json.format[Passcode]

@Singleton
class EmailVerificationPasscodesController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  emailVerificationPasscodesPage: EmailVerificationPasscodesPage,
  simplePage: SimplePage
)
extends FrontendController(mcc, actions):

  def showEmailVerificationPassCodes(emailVerificationLink: String): Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        if isSignedIn // hint: Using simple auth check since this is a test-only controller (avoiding custom auth action in production code)
        then
          httpClient
            .get(url"${appConfig.emailVerificationBaseUrl}/test-only/passcodes")
            .execute[Passcodes]
            .map: (passcodes: Passcodes) =>
              Ok(emailVerificationPasscodesPage(
                passcode = passcodes.passcodes.lastOption,
                emailVerificationLink = emailVerificationLink
              ))
        else Future.successful(BadRequest(simplePage("Email Verification Code (Test Only Page)", Some("You need to be signed in to access this page."))))
