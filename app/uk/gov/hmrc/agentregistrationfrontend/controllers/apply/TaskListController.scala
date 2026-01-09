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
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.TaskListStatus
import uk.gov.hmrc.agentregistrationfrontend.model.TaskStatus
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.TaskListPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskListController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  taskListPage: TaskListPage,
  businessPartnerRecordService: BusinessPartnerRecordService
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      _.agentApplication
        .isGrsDataReceived,
      implicit request =>
        logger.warn("Missing data from GRS, redirecting to start GRS registration")
        Redirect(AppRoutes.apply.AgentApplicationController.startRegistration)
    )
    .ensure(
      _.agentApplication
        .hasEntityCheckPassed.forall(_ === true),
      implicit request =>
        logger.warn("Entity failed or has not been completed, redirecting to entity check.")
        Redirect(AppRoutes.apply.internal.RefusalToDealWithController.check())
    )
    .async:
      implicit request =>
        businessPartnerRecordService
          .getBusinessPartnerRecord(request.agentApplication.getUtr)
          .map: bprOpt =>
            Ok(taskListPage(
              taskListStatus = request.agentApplication.taskListStatus,
              entityName = bprOpt
                .map(_.getEntityName)
                .getOrThrowExpectedDataMissing(
                  "Business Partner Record is missing"
                )
            ))

  extension (agentApplication: AgentApplication)

    def taskListStatus: TaskListStatus = {
      val contactIsComplete = agentApplication.applicantContactDetails.exists(_.isComplete)
      val amlsDetailsCompleted = agentApplication.amlsDetails.exists(_.isComplete)
      val agentDetailsIsComplete = agentApplication.agentDetails.exists(_.isComplete)
      val hmrcStandardForAgentsAgreed = agentApplication.hmrcStandardForAgentsAgreed === StateOfAgreement.Agreed
      TaskListStatus(
        contactDetails = TaskStatus(
          canStart = true, // Contact details can be started at any time
          isComplete = contactIsComplete
        ),
        amlsDetails = TaskStatus(
          canStart = true, // AMLS details can be started at any time
          isComplete = amlsDetailsCompleted
        ),
        agentDetails = TaskStatus(
          canStart = contactIsComplete, // Agent details can be started only when contact details are complete
          isComplete = agentDetailsIsComplete
        ),
        hmrcStandardForAgents = TaskStatus(
          canStart = true, // HMRC Standard for Agents can be started at any time
          isComplete = hmrcStandardForAgentsAgreed
        ),
        declaration = TaskStatus(
          canStart =
            contactIsComplete
              && amlsDetailsCompleted
              && agentDetailsIsComplete
              && hmrcStandardForAgentsAgreed, // Declaration can be started only when all prior tasks are complete
          isComplete = false // Declaration is never "complete" until submission
        )
      )
    }
