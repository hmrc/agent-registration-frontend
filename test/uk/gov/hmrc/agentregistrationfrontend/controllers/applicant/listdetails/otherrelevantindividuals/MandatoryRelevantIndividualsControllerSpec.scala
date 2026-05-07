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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.otherrelevantindividuals

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.OtherRelevantIndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class MandatoryRelevantIndividualsControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/tell-us-about-relevant-individuals"

  private val expectedHeading = "Tell us about the relevant individuals for Test Company Name"

  object agentApplication:

    val afterZeroKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterZeroKeyIndividuals

    val soleTraderInProgress =
      tdAll
        .agentApplicationSoleTrader
        .afterGrsDataReceived

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.otherrelevantindividuals.MandatoryRelevantIndividualsController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.otherrelevantindividuals.MandatoryRelevantIndividualsController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.otherrelevantindividuals.MandatoryRelevantIndividualsController.submit.url shouldBe
      AppRoutes.apply.listdetails.otherrelevantindividuals.MandatoryRelevantIndividualsController.show.url

  s"GET $path should redirect to task list when application is a sole trader" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.soleTraderInProgress)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header(HeaderNames.LOCATION).value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path should redirect to task list when application is a sole trader" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.soleTraderInProgress)
    val response: WSResponse =
      post(path)(Map(
        OtherRelevantIndividualNameForm.key -> Seq("Any Name")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header(HeaderNames.LOCATION).value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should return 200 and render mandatory relevant individuals page when zero key individuals" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterZeroKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterZeroKeyIndividuals.agentApplicationId,
      individuals = List.empty
    )

    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe s"$expectedHeading - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterZeroKeyIndividuals.agentApplicationId)

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterZeroKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterZeroKeyIndividuals.agentApplicationId,
      individuals = List.empty
    )

    val response: WSResponse = post(path)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe s"Error: $expectedHeading - Apply for an agent services account - GOV.UK"
    doc.mainContent
      .select(s"#${OtherRelevantIndividualNameForm.key}-error")
      .text() shouldBe "Error: Enter the full name of the person"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterZeroKeyIndividuals.agentApplicationId)

  s"POST $path with invalid inputs should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterZeroKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterZeroKeyIndividuals.agentApplicationId,
      individuals = List.empty
    )

    val response: WSResponse =
      post(path)(Map(
        OtherRelevantIndividualNameForm.key -> Seq("Invalid@@Name123")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe s"Error: $expectedHeading - Apply for an agent services account - GOV.UK"
    doc.mainContent
      .select(s"#${OtherRelevantIndividualNameForm.key}-error")
      .text() shouldBe "Error: The person’s name must only include letters a to z, hyphens, apostrophes and spaces"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterZeroKeyIndividuals.agentApplicationId)

  s"POST $path with save for later and valid input should redirect to save for later" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterZeroKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterZeroKeyIndividuals.agentApplicationId,
      individuals = List.empty
    )
    AgentRegistrationStubs.stubFindIndividualByPersonReferenceNoContent(tdAll.personReference)
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetailsAnyBody()

    val response: WSResponse =
      post(path)(Map(
        OtherRelevantIndividualNameForm.key -> Seq("Test Name"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with valid input should redirect to check your answers" in:
    ApplyStubHelper.stubsForSuccessfulUpdateWithBpr(
      agentApplication.afterZeroKeyIndividuals,
      agentApplication.afterZeroKeyIndividuals
    )
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterZeroKeyIndividuals.agentApplicationId,
      individuals = List.empty
    )
    AgentRegistrationStubs.stubFindIndividualByPersonReferenceNoContent(tdAll.personReference)
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetailsAnyBody()

    val response: WSResponse =
      post(path)(Map(
        OtherRelevantIndividualNameForm.key -> Seq("Test Name"),
        "submit" -> Seq("SaveAndContinue")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
