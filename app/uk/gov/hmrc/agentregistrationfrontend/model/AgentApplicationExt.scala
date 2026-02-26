/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.model

import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfRequiredKeyIndividuals
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

extension (agentApplication: AgentApplication)

  def taskListStatus(existingList: List[IndividualProvidedDetails]): TaskListStatus = {
    val contactIsComplete = agentApplication.applicantContactDetails.exists(_.isComplete)
    val amlsDetailsCompleted = agentApplication.amlsDetails.exists(_.isComplete)
    val agentDetailsIsComplete = agentApplication.agentDetails.exists(_.isComplete)
    val hmrcStandardForAgentsAgreed = agentApplication.hmrcStandardForAgentsAgreed === StateOfAgreement.Agreed

    def otherRelevantIndividualsComplete(existingList: List[IndividualProvidedDetails]): Boolean =
      agentApplication.hasOtherRelevantIndividuals match
        case Some(true) => existingList.exists(!_.isPersonOfControl)
        case Some(false) => true
        case None => false

    def listDetailsCompleted(existingList: List[IndividualProvidedDetails]): Boolean =
      agentApplication match
        case a: AgentApplication.IsAgentApplicationForDeclaringNumberOfKeyIndividuals =>
          NumberOfRequiredKeyIndividuals.isKeyIndividualListComplete(existingList.count(_.isPersonOfControl), a.numberOfRequiredKeyIndividuals)
          && otherRelevantIndividualsComplete(existingList)
        case _ => true

    val listProgressComplete = listDetailsCompleted(existingList) && existingList.forall(_.hasFinished)
    // any state other than Precreated indicates the link has been sent; require the list to be non-empty
    val listSharingComplete =
      listDetailsCompleted(existingList) &&
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
        canStart = contactIsComplete, // List details can be started only once we have a contact name
        isComplete = listDetailsCompleted(existingList)
      ),
      listShare = TaskStatus(
        canStart = listDetailsCompleted(existingList), // List sharing cannot be started until list details are completed
        isComplete = listSharingComplete
      ),
      listTracking = TaskStatus(
        canStart = listSharingComplete, // List tracking cannot be started until list share is complete
        isComplete = listProgressComplete
      ),
      declaration = TaskStatus(
        canStart =
          contactIsComplete
            && amlsDetailsCompleted
            && agentDetailsIsComplete
            && hmrcStandardForAgentsAgreed
            && listProgressComplete, // Declaration can be started only when all prior tasks are complete
        isComplete = false // Declaration is never "complete" until submission
      )
    )
  }
