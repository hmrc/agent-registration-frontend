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

package uk.gov.hmrc.agentregistrationfrontend.controllers

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistrationfrontend.action.{Actions, ApplicationRequest, AuthorisedUtrRequest}
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.forms.SelectFromOptionsForm
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessType
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.BusinessTypePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.TimedOutPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignOutController @Inject()(
  actions: Actions,
  mcc: MessagesControllerComponents,
  timedOutPage: TimedOutPage,
  appConfig: AppConfig
)
extends FrontendController(mcc):

  private def signOutWithContinue(continue: String) = {
    val signOutAndRedirectUrl: String = uri"""${appConfig.basFrontendSignOutUrlBase}?${Map("continue" -> continue)}""".toString
    Redirect(signOutAndRedirectUrl)
  }

  def signOut: Action[AnyContent] = Action {
    val continueUrl = uri"${appConfig.thisFrontendBaseUrl + routes.ApplicationController.landing.url}"
    signOutWithContinue(continueUrl.toString)
  }

  def timeOut: Action[AnyContent] = Action {
    val continueUrl = uri"${appConfig.thisFrontendBaseUrl + routes.SignOutController.timedOut.url}"
    signOutWithContinue(continueUrl.toString)
  }

  def timedOut: Action[AnyContent] = Action { implicit request =>
    Ok(timedOutPage())
  }
