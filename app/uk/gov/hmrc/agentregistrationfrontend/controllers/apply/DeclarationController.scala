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

import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.TaskListStatus
import uk.gov.hmrc.agentregistrationfrontend.model.TaskStatus
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.DeclarationPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeclarationController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: DeclarationPage,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  import actions.Applicant.*

  private val baseAction: ActionBuilderWithData[DataWithApplication] = actions
    .Applicant
    .getApplicationInProgress
    .ensure4(
      _.agentApplication.taskListStatus.declaration.canStart,
      implicit request =>
        logger.warn("Cannot start declaration whilst tasks are outstanding, redirecting to task list")
        Redirect(AppRoutes.apply.TaskListController.show)
    )

  def show: Action[AnyContent] = baseAction
    .getBusinessPartnerRecord:
      implicit request =>
        Ok(view(
          entityName = request.businessPartnerRecordResponse.getEntityName,
          agentApplication = request.agentApplication
        ))

  def submit: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        agentApplicationService
          .upsert(
            request.agentApplication
              .modify(_.applicationState)
              .setTo(ApplicationState.Submitted)
          ).map: _ =>
            Redirect(AppRoutes.apply.AgentApplicationController.applicationSubmitted)

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
        listDetails = TaskStatus(
          canStart = true, // List details can be started any time
          isComplete = false // TODO: implement list details so completion check can be done
        ),
        listShare = TaskStatus(
          canStart = false, // List sharing cannot be started until list details are completed
          isComplete = false // TODO: implement list share so completion check can be done
        ),
        listTracking = TaskStatus(
          canStart = false, // List tracking cannot be started until list share is complete
          isComplete = false // TODO: implement list details so completion check can be done
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
