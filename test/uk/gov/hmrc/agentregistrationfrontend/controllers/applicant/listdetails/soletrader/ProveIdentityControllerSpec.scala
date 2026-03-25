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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.soletrader

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class ProveIdentityControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/sole-trader"

  object agentApplication:

    val afterHmrcStandardForAgentsAgreed: AgentApplicationSoleTrader =
      tdAll
        .agentApplicationSoleTrader
        .afterHmrcStandardForAgentsAgreed

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.soletrader.ProveIdentityController.show shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path when no individual record exists should create individual record, return 200 and render page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHmrcStandardForAgentsAgreed)
    AgentRegistrationStubs.stubFindIndividualsForApplication(agentApplication.afterHmrcStandardForAgentsAgreed.agentApplicationId)
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(tdAll.providedDetails.soleTrader.soleTraderYetToProvideDetails)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Sign in with your personal details - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.stubFindIndividualsForApplication(agentApplication.afterHmrcStandardForAgentsAgreed.agentApplicationId)
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails()

  s"GET $path for a subsequent time should find existing individual record, return 200 and render page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHmrcStandardForAgentsAgreed)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHmrcStandardForAgentsAgreed.agentApplicationId,
      individuals = List(tdAll.providedDetails.soleTrader.soleTraderProvidedDetails)
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Sign in with your personal details - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.stubFindIndividualsForApplication(agentApplication.afterHmrcStandardForAgentsAgreed.agentApplicationId)
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails(0) // confirm we are not creating extra records on subsequent visits

  s"GET $path after individual record complete should return 200 and render page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHmrcStandardForAgentsAgreed)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHmrcStandardForAgentsAgreed.agentApplicationId,
      // TODO: Refactor this
      individuals = List(tdAll.providedDetails.soleTrader.soleTraderProvidedDetails.copy(providedDetailsState = ProvidedDetailsState.Finished))
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "You have proven your identity - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.stubFindIndividualsForApplication(agentApplication.afterHmrcStandardForAgentsAgreed.agentApplicationId)
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails(0) // confirm we are not creating extra records on subsequent visits
