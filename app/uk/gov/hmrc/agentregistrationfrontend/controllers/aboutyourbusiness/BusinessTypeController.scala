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
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.BusinessTypeSessionForm
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessTypeSessionValue
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.aboutyourbusiness.BusinessTypeSessionPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessTypeController @Inject() (
  mcc: MessagesControllerComponents,
  businessTypeSessionPage: BusinessTypeSessionPage
)
extends FrontendController(mcc):

  def show: Action[AnyContent] = Action:
    implicit request =>
      // ensure that agent type has been selected before allowing business type to be selected
      if request.readAgentType.isEmpty then
        Redirect(routes.AgentTypeController.show)
      else
        val form: Form[BusinessTypeSessionValue] =
          request.readBusinessType match
            case Some(bt: BusinessTypeSessionValue) => BusinessTypeSessionForm.form.fill(bt)
            case None => BusinessTypeSessionForm.form
        Ok(businessTypeSessionPage(form))

  def submit: Action[AnyContent] = Action:
    implicit request =>
      // ensure that agent type has been selected before allowing business type to be posted
      if request.readAgentType.isEmpty then
        Redirect(routes.AgentTypeController.show)
      else
        BusinessTypeSessionForm.form.bindFromRequest().fold(
          formWithErrors => BadRequest(businessTypeSessionPage(formWithErrors)),
          (businessType: BusinessTypeSessionValue) =>
            if businessType == BusinessTypeSessionValue.PartnershipType then
              Redirect(routes.PartnershipTypeController.show.url)
                .addBusinessTypeToSession(businessType)
            else if businessType == BusinessTypeSessionValue.NotSupported then
              Redirect(applicationRoutes.AgentApplicationController.genericExitPage.url)
                .addBusinessTypeToSession(businessType)
            else
              Redirect(applicationRoutes.AgentApplicationController.genericExitPage.url)
                .addBusinessTypeToSession(businessType) // TODO SoleTrader or LimitedCompany journeys not yet built
        )
