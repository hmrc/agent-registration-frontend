/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import play.api.mvc.*
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendControllerBase
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.validateRedirectUrl
import uk.gov.hmrc.agentregistrationfrontend.views.ErrorResults
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.NotAgentCredentialPage
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class NotAgentCredentialController @Inject() (
  mcc: MessagesControllerComponents,
  defaultActionBuilder: DefaultActionBuilder,
  af: AuthorisedFunctions,
  errorResults: ErrorResults,
  appConfig: AppConfig,
  view: NotAgentCredentialPage
)(using ExecutionContext)
extends FrontendControllerBase(mcc),
  RequestAwareLogging:

  def show(continueUrl: Option[RedirectUrl]): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      af.authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent).apply:
        val validatedContinueUrl: String = continueUrl
          .map(validateRedirectUrl(_, appConfig.allowedRedirectHosts))
          .getOrElse(appConfig.thisFrontendBaseUrl + AppRoutes.apply.AgentApplicationController.landing.url)

        val absoluteContinueUrl: String =
          if RedirectUrl.isRelativeUrl(validatedContinueUrl) then appConfig.thisFrontendBaseUrl + validatedContinueUrl
          else validatedContinueUrl

        val signInUrl =
          appConfig
            .signInUri(uri"$absoluteContinueUrl", AffinityGroup.Individual)
            .toString

        Future.successful(Ok(view(AppRoutes.SignOutController.signOutWithContinue(RedirectUrl(signInUrl)))))
      .recover:
        case _: NoActiveSession =>
          logger.info(s"Unauthorised because of 'NoActiveSession', redirecting to sign in page")
          Redirect(url = appConfig.signInUri(uri"""${appConfig.thisFrontendBaseUrl + request.uri}""", AffinityGroup.Agent).toString)
        case e: AuthorisationException =>
          logger.info(s"Unauthorised because of '${e.reason}', ${e.toString}")
          errorResults.unauthorised(message = e.toString)
