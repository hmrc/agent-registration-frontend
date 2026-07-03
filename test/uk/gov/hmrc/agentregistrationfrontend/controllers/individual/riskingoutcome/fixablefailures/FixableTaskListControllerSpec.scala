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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual.riskingoutcome.fixablefailures

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.ProvideDetailsStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class FixableTaskListControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private val path = s"/agent-registration/provide-details/conditions-not-yet-met/task-list/${tdAll.linkId.value}"
  object riskingOutcomeIndividual:
    val failingFixableAllCodes: RiskingOutcomeIndividual.FailedFixable =
      tdAll
        .riskingOutcomeIndividualFailedFixableAllCodes
  object agentApplication:
    val riskingCompletedFixable: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixable

  // these are the HTML document ids used in the fixable task list page
  object tasks:

    def individualFailures(index: Int) = s"fixableTasks-$index-status" // multiple tasks possible, index starts at 1

  "route should have correct path and method" in:
    AppRoutes.providedetails.riskingoutcome.fixablefailures.FixableTaskListController.show(tdAll.linkId) shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path for an individual with all failure codes should see the Incomplete status against each of them" in:
    ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
      agentApplication = agentApplication.riskingCompletedFixable,
      individualProvidedDetails = tdAll.providedDetails.afterFinished.copy(
        riskingOutcomeIndividual = Some(riskingOutcomeIndividual.failingFixableAllCodes)
      ),
      isScr = false
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Take action: You have not met the registration conditions - Apply for an agent services account - GOV.UK"
    for i <- riskingOutcomeIndividual.failingFixableAllCodes.fixes.indices do doc.getTaskStatus(tasks.individualFailures(i + 1)) shouldBe Constants.INCOMPLETE
    // in this task list the "declaration" task is not in its own group so will always be the last item
    doc.getTaskStatus(tasks.individualFailures(riskingOutcomeIndividual.failingFixableAllCodes.fixes.size + 1)) shouldBe Constants.CANNOT_START_YET
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()
