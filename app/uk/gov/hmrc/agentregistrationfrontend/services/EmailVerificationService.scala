/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.EmailVerificationConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.model.emailverification.*
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EmailVerificationService @Inject() (
  emailVerificationConnector: EmailVerificationConnector,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  def verifyEmail(
    credId: String,
    maybeEmail: Option[Email],
    continueUrl: String,
    maybeBackUrl: Option[String],
    accessibilityStatementUrl: String,
    lang: String
  )(using RequestHeader): Future[Result] =
    for {
      verifyEmailResponse <- emailVerificationConnector.verifyEmail(
        VerifyEmailRequest(
          credId = credId,
          continueUrl = continueUrl,
          origin =
            if (lang === "cy")
              "Gwasanaethau Asiant CThEM"
            else
              "HMRC Agent Services",
          deskproServiceName = None,
          accessibilityStatementUrl = accessibilityStatementUrl,
          email = maybeEmail,
          lang = Some(lang),
          backUrl = maybeBackUrl,
          pageTitle = None
        )
      )
    } yield
      val redirectToEmailVerificationUrl: String = appConfig.emailVerificationFrontendBaseUrl + verifyEmailResponse.redirectUri
      if appConfig.injectEmailVerificationPasscodesPage
      then
        Redirect(AppRoutes.testOnly.EmailVerificationPasscodesController.showEmailVerificationPassCodes(emailVerificationLink = redirectToEmailVerificationUrl))
      else Redirect(redirectToEmailVerificationUrl)

  def checkEmailVerificationStatus(
    credId: String,
    email: String
  )(using RequestHeader): Future[EmailVerificationStatus] = emailVerificationConnector
    .checkEmailVerificationStatus(credId)
    .map(
      _.emails.find(_.emailAddress === email.toLowerCase)
        .fold[EmailVerificationStatus](EmailVerificationStatus.Unverified)(completedEmail =>
          if completedEmail.verified then EmailVerificationStatus.Verified
          else if completedEmail.locked then EmailVerificationStatus.Locked
          else EmailVerificationStatus.Unverified
        )
    )
