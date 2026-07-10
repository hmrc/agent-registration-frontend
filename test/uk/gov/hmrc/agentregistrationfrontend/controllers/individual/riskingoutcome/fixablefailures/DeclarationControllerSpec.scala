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

import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.ProvideDetailsStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class DeclarationControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private val path = s"/agent-registration/provide-details/conditions-not-yet-met/declaration/${tdAll.linkId.value}"
  object riskingOutcomeIndividual:

    val beforeDeclaration: RiskingOutcomeIndividual.FailedFixable =
      tdAll
        .beforeDeclaration
    val withUnfixedFixes: RiskingOutcomeIndividual.FailedFixable =
      tdAll
        .riskingOutcomeIndividualFailedFixableAllCodes

  object agentApplication:
    val riskingCompletedFixable: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixable

  "route should have correct path and method" in:
    AppRoutes.providedetails.riskingoutcome.fixablefailures.DeclarationController.show(tdAll.linkId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.riskingoutcome.fixablefailures.DeclarationController.submit(tdAll.linkId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.riskingoutcome.fixablefailures.DeclarationController.show(tdAll.linkId).url shouldBe
      AppRoutes.providedetails.riskingoutcome.fixablefailures.DeclarationController.submit(tdAll.linkId).url

  s"GET $path for an individual with all fixes complete should return 200 and render the page" in:
    ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
      agentApplication = agentApplication.riskingCompletedFixable,
      individualProvidedDetails = tdAll.providedDetails.afterFinished.copy(
        riskingOutcomeIndividual = Some(riskingOutcomeIndividual.beforeDeclaration)
      ),
      isScr = false
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "You are about to submit your responses - Apply for an agent services account - GOV.UK"
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()

  s"GET $path for an individual without all fixes complete should redirect to the risking outcome page" in:
    ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
      agentApplication = agentApplication.riskingCompletedFixable,
      individualProvidedDetails = tdAll.providedDetails.afterFinished.copy(
        riskingOutcomeIndividual = Some(riskingOutcomeIndividual.withUnfixedFixes)
      ),
      isScr = false
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header(HeaderNames.LOCATION) shouldBe Some(
      AppRoutes.providedetails.riskingoutcome.RiskingOutcomeController.show(tdAll.linkId).url
    )
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()

  s"POST $path for an individual with all fixes complete should update the record and redirect to the confirmation page" in:
    ProvideDetailsStubHelper.stubAuthAndFixIndividualProvidedDetails(
      agentApplication = agentApplication.riskingCompletedFixable,
      individualProvidedDetails = tdAll.providedDetails.afterFinished.copy(
        riskingOutcomeIndividual = Some(riskingOutcomeIndividual.beforeDeclaration)
      ),
      updatedIndividualProvidedDetails = tdAll.providedDetails.afterFinished.copy(
        riskingOutcomeIndividual = Some(riskingOutcomeIndividual.beforeDeclaration.copy(declarationAgreed = true))
      )
    )
    val response: WSResponse = post(path)(Map.empty)

    response.status shouldBe Status.SEE_OTHER
    response.header(HeaderNames.LOCATION) shouldBe Some(
      AppRoutes.providedetails.riskingoutcome.fixablefailures.IndividualConfirmationController.show(tdAll.linkId).url
    )
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()
