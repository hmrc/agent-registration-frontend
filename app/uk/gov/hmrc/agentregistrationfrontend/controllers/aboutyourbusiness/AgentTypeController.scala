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

package uk.gov.hmrc.agentregistrationfrontend.controllers.aboutyourbusiness

import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Request
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentTypeForm
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.aboutyourbusiness.AgentTypePage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentTypeController @Inject() (
  mcc: MessagesControllerComponents,
  view: AgentTypePage
)
extends FrontendController(mcc):

  def show: Action[AnyContent] = Action:
    implicit request: Request[?] =>
      val form: Form[AgentType] =
        request.readAgentType match
          case Some(value: AgentType) => AgentTypeForm.form.fill(value)
          case _ => AgentTypeForm.form
      Ok(view(form))

  def submit: Action[AnyContent] = Action:
    implicit request: Request[?] =>
      AgentTypeForm.form.bindFromRequest().fold(
        formWithErrors => BadRequest(view(formWithErrors)),
        (agentType: AgentType) =>
          if agentType == AgentType.UkTaxAgent
          then
            Redirect(routes.BusinessTypeController.show.url)
              .addAgentTypeToSession(agentType)
          else Redirect(applicationRoutes.AgentApplicationController.genericExitPage.url)
      )
