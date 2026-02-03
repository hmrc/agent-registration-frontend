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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.ConfirmationPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.ViewApplicationPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentApplicationController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  simplePage: SimplePage,
  confirmationPage: ConfirmationPage,
  viewApplicationPage: ViewApplicationPage
)
extends FrontendController(mcc, actions):

  // TODO: is this endpoint really needed?
  def landing: Action[AnyContent] = actions
    .Applicant
    .getApplicationInProgress:
      implicit request =>
        // until we have more than the registration journey just go to the task list
        // which will redirect to the start of registration if needed
        Redirect(AppRoutes.apply.TaskListController.show)

  // TODO: is this endpoint really needed?
  def applicationDashboard: Action[AnyContent] = actions
    .Applicant
    .getApplicationInProgress:
      implicit request =>
        Ok(simplePage(
          h1 = "Application Dashboard page...",
          bodyText = Some(
            "Placeholder for the Application Dashboard page..."
          )
        ))

  def applicationSubmitted: Action[AnyContent] = actions
    .Applicant
    .getApplicationSubmitted4
    .getBusinessPartnerRecord:
      implicit request =>
        Ok(confirmationPage(
          entityName = request.businessPartnerRecordResponse.getEntityName,
          agentApplication = request.get[AgentApplication]
        ))

  def viewSubmittedApplication: Action[AnyContent] = actions
    .Applicant
    .getApplicationSubmitted4
    .getBusinessPartnerRecord:
      implicit request =>
        Ok(viewApplicationPage(
          entityName = request.businessPartnerRecordResponse.getEntityName,
          agentApplication = request.get[AgentApplication]
        ))

  def startRegistration: Action[AnyContent] = actions.action:
    implicit request =>
      // if we use an endpoint like this, we can later change the flow without changing the URL
      Redirect(AppRoutes.apply.aboutyourbusiness.AgentTypeController.show)

  def genericExitPage: Action[AnyContent] = actions.action:
    implicit request =>
      Ok(simplePage(
        h1 = "You cannot use this service...",
        bodyText = Some(
          "Placeholder for the generic exit page..."
        )
      ))
