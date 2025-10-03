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
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.BusinessTypeSessionForm
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessTypeAnswer
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.BusinessTypeSessionPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessTypeSessionController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  businessTypeSessionPage: BusinessTypeSessionPage
)
extends FrontendController(mcc, actions):

  private val baseAction = action
    .ensure(
      _.readAgentType.isDefined,
      implicit request =>
        logger.warn("Agent type not selected - redirecting to agent type selection page")
        Redirect(routes.AgentTypeController.show)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val form: Form[BusinessTypeAnswer] =
        request.readBusinessType match
          case Some(bt: BusinessTypeAnswer) => BusinessTypeSessionForm.form.fill(bt)
          case None => BusinessTypeSessionForm.form
      Ok(businessTypeSessionPage(form))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidForm(BusinessTypeSessionForm.form, implicit r => businessTypeSessionPage(_)):
        implicit request =>
          request.formValue match
            case businessType @ (BusinessTypeAnswer.SoleTrader | BusinessTypeAnswer.LimitedCompany) =>
              // TODO SoleTrader or LimitedCompany journeys not yet built
              Redirect(applicationRoutes.AgentApplicationController.genericExitPage.url)
                .addBusinessTypeAnswerToSession(businessType)
                .removePartnershipTypeFromSession
            case businessType @ BusinessTypeAnswer.PartnershipType =>
              Redirect(routes.PartnershipTypeController.show.url)
                .addBusinessTypeAnswerToSession(businessType)
            case businessType @ BusinessTypeAnswer.Other =>
              Redirect(applicationRoutes.AgentApplicationController.genericExitPage.url)
                .addBusinessTypeAnswerToSession(businessType)
