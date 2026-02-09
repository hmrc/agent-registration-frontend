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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant

import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.HmrcStandardForAgentsPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HmrcStandardForAgentsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: HmrcStandardForAgentsPage,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] = actions
    .getApplicationInProgress
    .getBusinessPartnerRecord:
      implicit request =>
        Ok(view(
          entityName = request.businessPartnerRecordResponse.getEntityName,
          agentApplication = request.agentApplication
        ))

  def submit: Action[AnyContent] = actions
    .getApplicationInProgress
    .async:
      implicit request =>
        agentApplicationService
          .upsert(
            request
              .agentApplication
              .modify(_.hmrcStandardForAgentsAgreed)
              .setTo(StateOfAgreement.Agreed)
          ).map: _ =>
            Redirect(AppRoutes.apply.TaskListController.show)
