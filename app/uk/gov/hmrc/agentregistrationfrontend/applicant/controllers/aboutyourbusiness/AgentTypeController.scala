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

package uk.gov.hmrc.agentregistrationfrontend.applicant.controllers.aboutyourbusiness

import play.api.data.Form
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentTypeForm
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.shared.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.AgentTypePage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentTypeController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: AgentTypePage
)
extends FrontendController(mcc, actions):

  def show: Action[?] = action:
    implicit request: Request[?] =>
      val form: Form[AgentType] =
        request.readAgentType match
          case Some(value: AgentType) => AgentTypeForm.form.fill(value)
          case _ => AgentTypeForm.form
      Ok(view(form))

  def submit: Action[AnyContent] =
    action
      .ensureValidForm(AgentTypeForm.form, implicit request => view(_)):
        implicit request =>
          val agentType: AgentType = request.formValue
          val call: Call =
            agentType match
              case AgentType.UkTaxAgent => AppRoutes.apply.aboutyourbusiness.BusinessTypeSessionController.show
              case AgentType.NonUkTaxAgent => AppRoutes.apply.AgentApplicationController.genericExitPage
          Redirect(call.url).addToSession(agentType)
