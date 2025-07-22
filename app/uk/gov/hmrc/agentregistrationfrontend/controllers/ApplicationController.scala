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
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.ApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedUtrRequest
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplicationController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  applicationService: ApplicationService,
  simplePage: SimplePage
)
extends FrontendController(mcc):

  val initializeApplication: Action[AnyContent] = actions.authorisedUtr.async { implicit request =>
    applicationService
      .upsertNewApplication()
      .map(_ => Redirect(routes.ApplicationController.landing.url))
  }

  val landing: Action[AnyContent] = actions.getApplicationInProgress.async { implicit request =>
    Future.successful(Ok(simplePage(
      h1 = "Landing page...",
      bodyText = Some(
        "Placeholder for the landing page..."
      )
    )))
  }

  val applicationSubmitted: Action[AnyContent] = actions.getApplicationSubmitted.async { implicit request =>
    Future.successful(Ok(simplePage(
      h1 = "Application Submitted",
      bodyText = Some(
        "Placeholder for the application submitted page..."
      )
    )))
  }

  def startRegistration: Action[AnyContent] = Action { implicit request =>
    // if we use an endpoint like this, we can later change the flow without changing the URL
    Redirect(routes.BusinessTypeController.show)
  }
