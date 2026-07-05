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
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.ProvideDetailsStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class FixIndividualProvidedDetailsControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private val path = s"/agent-registration/provide-details/conditions-not-yet-met/identity/${tdAll.linkId.value}"

  object riskingOutcomeIndividual:
    val failingFixableAllCodes: RiskingOutcomeIndividual.FailedFixable =
      tdAll
        .riskingOutcomeIndividualDetailsFix

  object agentApplication:

    def riskingCompletedFailedFixableAllCodes: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixableAllCodes

  final case class TestCase(
    name: String,
    application: AgentApplication,
    fixes: Seq[IndividualFix],
    headings: Map[String, String]
  )
  List(
    TestCase(
      name = "Individual with all fixable failure codes",
      application = agentApplication.riskingCompletedFailedFixableAllCodes,
      fixes = riskingOutcomeIndividual.failingFixableAllCodes.fixes,
      headings = Map(
        "IndividualFix.10.IndividualDetailsFix" -> "We could not match the details you provided with an HMRC record"
      )
    )
  ).foreach: testCase =>
    testCase.fixes.foreach: fix =>
      val (individualFixCode: String, individualFixHeading: String) = (fix.toString, testCase.headings(fix.toString))
      s"route for ${testCase.name} with individual fix code $individualFixCode should have correct path and method" in:
        AppRoutes.providedetails.riskingoutcome.fixablefailures.FixIndividualProvidedDetailsController.show(tdAll.linkId) shouldBe Call(
          method = "GET",
          url = path
        )

      s"GET $path for ${testCase.name} should render correct content" in:
        ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
          agentApplication = testCase.application,
          individualProvidedDetails = tdAll.providedDetails.afterFinished.copy(
            riskingOutcomeIndividual = Some(riskingOutcomeIndividual.failingFixableAllCodes)
          ),
          isScr = false
        )
        val response: WSResponse = get(path)

        response.status shouldBe Status.OK
        val doc = response.parseBodyAsJsoupDocument
        doc.title() shouldBe s"$individualFixHeading - Apply for an agent services account - GOV.UK"
        doc.mainContent.select("a.govuk-button").attr("href") shouldBe
          AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.CheckYourAnswersController.show(
            tdAll.linkId
          ).url withClue "Page should link to provided details CYA"
