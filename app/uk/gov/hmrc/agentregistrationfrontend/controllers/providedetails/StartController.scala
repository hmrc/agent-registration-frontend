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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes
import uk.gov.hmrc.agentregistrationfrontend.services.AgentRegistrationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.StartPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  startPage: StartPage,
  applicationService: AgentRegistrationService
)
extends FrontendController(mcc, actions):

  def start(linkId: String): Action[AnyContent] = Action
    .async:
      implicit request: RequestHeader =>
        applicationService.findApplicationByLinkId(LinkId(linkId)).map {
          case Some(app) if app.hasFinished => Ok(startPage(app))
          case _ => Redirect(routes.AgentApplicationController.genericExitPage.url)
        }
