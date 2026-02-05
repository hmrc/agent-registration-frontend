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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.aboutyourbusiness

import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistrationfrontend.action.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.BusinessTypeSessionForm
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessTypeAnswer
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.BusinessTypeSessionPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessTypeSessionController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  businessTypeSessionPage: BusinessTypeSessionPage
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[AgentType *: EmptyTuple] = action
    .refineWithData:
      implicit request =>
        request.readFromSessionAgentType match
          case Some(agentType: AgentType) => request.add[AgentType](agentType)
          case None =>
            logger.warn("Agent type not selected - redirecting to agent type selection page")
            Redirect(AppRoutes.apply.aboutyourbusiness.AgentTypeController.show)

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val form: Form[BusinessTypeAnswer] =
        request.readBusinessTypeAnswer match
          case Some(bt: BusinessTypeAnswer) => BusinessTypeSessionForm.form.fill(bt)
          case None => BusinessTypeSessionForm.form
      Ok(businessTypeSessionPage(form))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidForm(BusinessTypeSessionForm.form, implicit r => businessTypeSessionPage(_)):
        implicit request =>
          request.get[BusinessTypeAnswer] match
            case businessType @ BusinessTypeAnswer.LimitedCompany =>
              Redirect(AppRoutes.apply.aboutyourbusiness.UserRoleController.show.url)
                .addToSession(businessType)
                .removePartnershipTypeFromSession
            case businessType @ BusinessTypeAnswer.SoleTrader =>
              Redirect(AppRoutes.apply.aboutyourbusiness.UserRoleController.show.url)
                .addToSession(businessType)
                .removePartnershipTypeFromSession
            case businessType @ BusinessTypeAnswer.PartnershipType =>
              Redirect(AppRoutes.apply.aboutyourbusiness.PartnershipTypeController.show.url)
                .addToSession(businessType)
            case businessType @ BusinessTypeAnswer.Other =>
              Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)
                .addToSession(businessType)
