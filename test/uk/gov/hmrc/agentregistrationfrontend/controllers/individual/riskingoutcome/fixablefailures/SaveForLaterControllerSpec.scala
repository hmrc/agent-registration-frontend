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
import uk.gov.hmrc.agentregistrationfrontend.util.DisplayDate

class SaveForLaterControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private val path = s"/agent-registration/provide-details/conditions-not-yet-met/save-and-come-back-later/${tdAll.linkId.value}"
  private val correctiveActionExpiryDate: String = DisplayDate.displayDateForLang(
    Some(tdAll.riskingOutcomeApplication.failedFixable.correctiveActionExpiryDate)
  )

  object riskingOutcomeIndividual:
    val failingFixableAllCodes: RiskingOutcomeIndividual.FailedFixable =
      tdAll
        .riskingOutcomeIndividualFailedFixableAllCodes

  object agentApplication:

    val riskingCompletedFixable: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixable
    val afterResubmitted: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterResubmitted
    val nonFixableOutcome: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedNonFixable

  "route should have correct path and method" in:
    AppRoutes.providedetails.riskingoutcome.fixablefailures.SaveForLaterController.show(tdAll.linkId) shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path when application outcome is failed fixable should render page" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication = agentApplication.riskingCompletedFixable,
      individualProvideDetails = tdAll.providedDetails.afterFinished.copy(
        riskingOutcomeIndividual = Some(riskingOutcomeIndividual.failingFixableAllCodes)
      ),
      withBpr = true
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe s"Your progress will be saved until $correctiveActionExpiryDate - Apply for an agent services account - GOV.UK"
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails(withBpr = true)

  s"GET $path when application outcome is not failed fixable should redirect to the risking outcome page" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication = agentApplication.nonFixableOutcome,
      individualProvideDetails = tdAll.providedDetails.afterFinished.copy(
        riskingOutcomeIndividual = Some(riskingOutcomeIndividual.failingFixableAllCodes)
      ),
      withBpr = true
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location") shouldBe Some(
      AppRoutes.providedetails.riskingoutcome.RiskingOutcomeController.show(tdAll.linkId).url
    )
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails(withBpr = true)

  s"GET $path for already resubmitted should redirect to risking outcome status endpoint" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication = agentApplication.afterResubmitted,
      individualProvideDetails = tdAll.providedDetails.afterFinished.copy(
        riskingOutcomeIndividual = Some(riskingOutcomeIndividual.failingFixableAllCodes)
      ),
      withBpr = true
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location") shouldBe Some(
      AppRoutes.providedetails.riskingoutcome.RiskingOutcomeController.show(tdAll.linkId).url
    )
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails(withBpr = true)
