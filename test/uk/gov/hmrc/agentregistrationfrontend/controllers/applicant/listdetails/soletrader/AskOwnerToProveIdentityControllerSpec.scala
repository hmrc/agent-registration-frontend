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
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class AskOwnerToProveIdentityControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/ask-sole-trader"

  object agentApplication:

    val afterHmrcStandardForAgentsAgreed: AgentApplicationSoleTrader = tdAll
      .agentApplicationSoleTrader
      .afterHmrcStandardForAgentsAgreed
      .copy(userRole = Some(UserRole.Authorised))

  object individualProvidedDetails:

    val precreated: IndividualProvidedDetails = tdAll.providedDetails.precreated.copy(individualName = IndividualName("ST Name ST Lastname"))
    val claimed: IndividualProvidedDetails = tdAll.providedDetails.afterAccessConfirmed

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.soletrader.AskOwnerToProveIdentityController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.soletrader.AskOwnerToProveIdentityController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.soletrader.AskOwnerToProveIdentityController.show.url shouldBe
      AppRoutes.apply.listdetails.soletrader.AskOwnerToProveIdentityController.submit.url

  s"GET $path when no individual record exists should create individual record, return 200 and render page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHmrcStandardForAgentsAgreed)
    AgentRegistrationStubs.stubFindIndividualsForApplication(agentApplication.afterHmrcStandardForAgentsAgreed.agentApplicationId)
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(individualProvidedDetails.precreated)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Share this link with the business owner - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.stubFindIndividualsForApplication(agentApplication.afterHmrcStandardForAgentsAgreed.agentApplicationId)
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails()

  s"GET $path for a subsequent time should find existing individual record, return 200 and render page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHmrcStandardForAgentsAgreed)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHmrcStandardForAgentsAgreed.agentApplicationId,
      individuals = List(individualProvidedDetails.claimed)
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Share this link with the business owner - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.stubFindIndividualsForApplication(agentApplication.afterHmrcStandardForAgentsAgreed.agentApplicationId)
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails(0) // confirm we are not creating extra records on subsequent visits

  s"POST $path should update individual record as having access confirmed and redirect to task list page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHmrcStandardForAgentsAgreed)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHmrcStandardForAgentsAgreed.agentApplicationId,
      individuals = List(individualProvidedDetails.claimed)
    )
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
      individualProvidedDetails.claimed.copy(providedDetailsState = ProvidedDetailsState.AccessConfirmed)
    )
    val response: WSResponse =
      post(path)(
        Map("submit" -> Seq("SaveAndContinue"))
      )

    response.status shouldBe Status.SEE_OTHER
    response.header("Location") shouldBe Some(AppRoutes.apply.TaskListController.show.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterHmrcStandardForAgentsAgreed.agentApplicationId)
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails()
