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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix._3.AmlsFix
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class FixableTaskListControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private val path = "/agent-registration/conditions-not-yet-met/task-list"
  object agentApplication:
    val riskingCompletedFixable: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixable

  // these are the HTML document ids used in the fixable task list page
  object tasks:

    val amls = "amlsDetails-1-status" // only ever one task
    def entityFailures(index: Int) = s"entityFailures-$index-status" // multiple tasks possible, index starts at 1
    val checkProgressOfIndividuals = "individualFailures-1-status" // only ever one task
    val declaration = "declaration-1-status"

  "route should have correct path and method" in:
    AppRoutes.fixablefailures.FixableTaskListController.show shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path for fixes including individuals and Amls fix should render correct status tags" in:
    val expectedEntityFixes: Seq[EntityFix] = tdAll.riskingOutcomeEntityFailedFixable(isFixed = None).fixes.filterNot {
      case _: AmlsFix => true
      case _ => false
    }
    ApplyStubHelper.stubsForApplicationBprAndIndividuals(
      application = agentApplication.riskingCompletedFixable,
      individuals = List(
        tdAll.providedDetails.afterFinished.copy(riskingOutcomeIndividual =
          Some(RiskingOutcomeIndividual.FailedFixable(
            fixes = Seq(
              IndividualFix._4._1(isConfirmed = None)
            ),
            declarationAgreed = false
          ))
        )
      )
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Take action: Test Company Name has not met the registration conditions - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.amls) shouldBe Constants.INCOMPLETE
    expectedEntityFixes.zipWithIndex.map: (_, index) =>
      doc.getTaskStatus(tasks.entityFailures(index + 1)) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.checkProgressOfIndividuals) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.CANNOT_START_YET
    ApplyStubHelper.verifyConnectorsForApplicationBprAndIndividuals(agentApplication.riskingCompletedFixable)
