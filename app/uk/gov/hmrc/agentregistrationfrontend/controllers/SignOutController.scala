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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.views.html.TimedOutPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignOutController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  timedOutPage: TimedOutPage,
  appConfig: AppConfig
)
extends FrontendController(mcc, actions):

  private def signOutWithContinue(continue: String): Result =
    val signOutAndRedirectUrl: String = uri"""${appConfig.basFrontendSignOutUrlBase}?${Map("continue" -> continue)}""".toString
    Redirect(signOutAndRedirectUrl)

  def signOut: Action[AnyContent] = action:
    val continueUrl = uri"${appConfig.thisFrontendBaseUrl + routes.AgentApplicationController.landing.url}"
    signOutWithContinue(continueUrl.toString)

  def timeOut: Action[AnyContent] = action:
    val continueUrl = uri"${appConfig.thisFrontendBaseUrl + routes.SignOutController.timedOut.url}"
    signOutWithContinue(continueUrl.toString)

  def timedOut: Action[AnyContent] = action:
    implicit request =>
      Ok(timedOutPage())
