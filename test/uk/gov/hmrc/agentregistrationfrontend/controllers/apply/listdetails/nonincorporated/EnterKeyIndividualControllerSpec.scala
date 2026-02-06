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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.listdetails.nonincorporated

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class EnterKeyIndividualControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/enter-key-individual"

  object agentApplication:

    val beforeHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHmrcStandardForAgentsAgreed

    val afterHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

    val afterOnlyOneKeyIndividual: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterOnlyOneKeyIndividual

    // SixOrMore selected and 5 or more nominated
    val afterHowManyKeyIndividualsNeedsNoPadding: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividualsNeedsNoPadding

    // SixOrMore selected and less than 5 nominated
    val afterHowManyKeyIndividualsNeedsPadding: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividualsNeedsPadding

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.nonincorporated.EnterKeyIndividualController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.nonincorporated.EnterKeyIndividualController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.nonincorporated.EnterKeyIndividualController.submit.url shouldBe
      AppRoutes.apply.listdetails.nonincorporated.EnterKeyIndividualController.show.url

  s"GET $path should return 200, fetch the BPR and render page for entering first partner" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHowManyKeyIndividuals.agentApplicationId,
      individuals = List.empty // existing list is empty and target size is 3, so this is for the "first" partner
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What is the full name of the first partner? - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should return 200, fetch the BPR and render page for entering next partner" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHowManyKeyIndividuals.agentApplicationId,
      individuals = List(tdAll.individualProvidedDetails) // existing list already has one individual so this is the "next" partner
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What is the full name of the next partner? - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should return 200, fetch the BPR and render page for entering the only partner" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterOnlyOneKeyIndividual)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHowManyKeyIndividuals.agentApplicationId,
      individuals = List.empty // even though this makes this the "first" entry, there is only one required so the "only" partner heading is shown
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What is the full name of the partner? - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHowManyKeyIndividuals.agentApplicationId,
      individuals = List.empty
    )
    val response: WSResponse = post(path)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is the full name of the first partner? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${IndividualNameForm.key}-error"
    ).text() shouldBe "Error: Enter the full name of the partner"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with invalid inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHowManyKeyIndividuals.agentApplicationId,
      individuals = List.empty
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualNameForm.key -> Seq("Invalid@@Name123")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is the full name of the first partner? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${IndividualNameForm.key}-error"
    ).text() shouldBe "Error: The partner’s name must only include letters a to z, hyphens, apostrophes and spaces"
    ApplyStubHelper.verifyConnectorsForAuthAction()
