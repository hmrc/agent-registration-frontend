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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.hasCheckPassed
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.llp.ProvidedDetailsState
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.model.TaskListStatus
import uk.gov.hmrc.agentregistrationfrontend.model.TaskStatus
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.TaskListPage
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskListController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  taskListPage: TaskListPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] = actions
    .getApplicationInProgress
    .ensure(
      condition = _.agentApplication.isGrsDataReceived,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Missing data from GRS, redirecting to start GRS registration")
          Redirect(AppRoutes.apply.AgentApplicationController.startRegistration)
    )
    .ensure(
      condition = _.agentApplication.hasCheckPassed,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Entity failed or has not been completed, redirecting to entity check.")
          Redirect(AppRoutes.apply.internal.RefusalToDealWithController.check())
    )
    .refine(implicit request =>
      val agentApplication: AgentApplication = request.get
      individualProvideDetailsService.findAllByApplicationId(agentApplication.agentApplicationId).map: individualsList =>
        request.add[List[IndividualProvidedDetails]](individualsList)
    )
    .getBusinessPartnerRecord:
      implicit request =>
        val agentApplication: AgentApplication = request.get
        Ok(taskListPage(
          taskListStatus = agentApplication.taskListStatus(existingList = request.get[List[IndividualProvidedDetails]]),
          entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
          agentApplication = agentApplication
        ))

  extension (agentApplication: AgentApplication)

    def taskListStatus(existingList: List[IndividualProvidedDetails]): TaskListStatus = {
      val contactIsComplete = agentApplication.applicantContactDetails.exists(_.isComplete)
      val amlsDetailsCompleted = agentApplication.amlsDetails.exists(_.isComplete)
      val agentDetailsIsComplete = agentApplication.agentDetails.exists(_.isComplete)
      val hmrcStandardForAgentsAgreed = agentApplication.hmrcStandardForAgentsAgreed === StateOfAgreement.Agreed
      val listDetailsCompleted = existingList.nonEmpty // TODO: implement other relevant individuals list so completion check can be done
      // any state other than Precreated indicates the link has been sent; require the list to be non-empty
      val listSharingComplete =
        listDetailsCompleted &&
          existingList.forall(_.providedDetailsState =!= ProvidedDetailsState.Precreated)
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
          isComplete = listDetailsCompleted // TODO: implement other relevant individuals list details so completion check can be done
        ),
        listShare = TaskStatus(
          canStart = listDetailsCompleted, // List sharing cannot be started until list details are completed
          isComplete = listSharingComplete
        ),
        listTracking = TaskStatus(
          canStart = listSharingComplete, // List tracking cannot be started until list share is complete
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
