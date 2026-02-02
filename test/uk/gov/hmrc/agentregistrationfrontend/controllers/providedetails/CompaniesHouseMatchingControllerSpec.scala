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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails

import com.softwaremill.quicklens.modify
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistrationfrontend.forms.ChOfficerSelectionForms
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.CompaniesHouseStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs

class CompaniesHouseMatchingControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/provide-details/name-match"

  object agentApplication:

    val inComplete: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterContactTelephoneSelected

    val applicationSubmitted: AgentApplication = inComplete
      .modify(_.applicationState)
      .setTo(ApplicationState.Submitted)

  private object memberProvidedDetails:

    val afterStarted: IndividualProvidedDetailsToBeDeleted =
      tdAll
        .providedDetailsLlp
        .afterStarted

    val afterNameQueryProvided: IndividualProvidedDetailsToBeDeleted =
      tdAll
        .providedDetailsLlp
        .afterNameQueryProvided

    val afterOfficerChosen: IndividualProvidedDetailsToBeDeleted =
      tdAll
        .providedDetailsLlp
        .afterOfficerChosen

  private val lastName = "Leadenhall-Lane"

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.CompaniesHouseMatchingController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.CompaniesHouseMatchingController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.CompaniesHouseMatchingController.submit.url shouldBe AppRoutes.providedetails.CompaniesHouseMatchingController.show.url

  s"GET $path should return 200 and render page when there is a single match" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(memberProvidedDetails.afterNameQueryProvided))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)
    CompaniesHouseStubs.stubSingleMatch(lastName = lastName)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Are these your details? - Apply for an agent services account - GOV.UK"

  s"GET $path should return 200 and render page when there are multiple matches" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(memberProvidedDetails.afterNameQueryProvided))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)
    CompaniesHouseStubs.stubMultipleMatches(lastName = lastName)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "2 records match this name - Apply for an agent services account - GOV.UK"

  s"GET $path should redirect to name query page when name query is missing" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(memberProvidedDetails.afterStarted))
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.providedetails.CompaniesHouseNameQueryController.show.url
    AuthStubs.verifyAuthorise()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()

  s"POST $path for single match without a valid selection should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(memberProvidedDetails.afterNameQueryProvided))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)
    CompaniesHouseStubs.stubSingleMatch(lastName = lastName)
    val response: WSResponse =
      post(path)(Map(
        "ChOfficerSelectionFormType" -> Seq("YesNoForm")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Are these your details? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#companiesHouseOfficer-error").text() shouldBe "Error: Select yes if these are your details"
    AuthStubs.verifyAuthorise()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()
    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(tdAll.agentApplicationId)
    CompaniesHouseStubs.verifySingleMatchCalls(lastName = lastName)

  s"POST $path for single match with valid inputs should save officer and redirect to telephone page" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(memberProvidedDetails.afterNameQueryProvided))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)
    CompaniesHouseStubs.stubSingleMatch(lastName = lastName)
    AgentRegistrationIndividualProvidedDetailsStubs.stubUpsertIndividualProvidedDetails(memberProvidedDetails.afterOfficerChosen)
    val response: WSResponse =
      post(path)(Map(
        "ChOfficerSelectionFormType" -> Seq("YesNoForm"),
        "companiesHouseOfficer" -> Seq("Yes")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show.url
    AuthStubs.verifyAuthorise()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()
    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(tdAll.agentApplicationId)
    CompaniesHouseStubs.verifySingleMatchCalls(lastName = lastName)

  s"POST $path for multiple matches without a valid selection should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(memberProvidedDetails.afterNameQueryProvided))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)
    CompaniesHouseStubs.stubMultipleMatches(lastName = lastName)
    val response: WSResponse =
      post(path)(Map(
        "ChOfficerSelectionFormType" -> Seq("OfficerSelectionForm")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: 2 records match this name - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#companiesHouseOfficer-error").text() shouldBe "Error: Select the name and date of birth that matches your details"
    AuthStubs.verifyAuthorise()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()
    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(tdAll.agentApplicationId)
    CompaniesHouseStubs.verifyMultipleMatchCalls(lastName = lastName)

  s"POST $path for multiple matches with valid inputs should save officer and redirect to telephone page" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(memberProvidedDetails.afterNameQueryProvided))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)
    CompaniesHouseStubs.stubMultipleMatches(lastName = lastName)
    AgentRegistrationIndividualProvidedDetailsStubs.stubUpsertIndividualProvidedDetails(memberProvidedDetails.afterOfficerChosen)
    val response: WSResponse =
      post(path)(Map(
        "ChOfficerSelectionFormType" -> Seq("OfficerSelectionForm"),
        ChOfficerSelectionForms.key -> Seq("Taylor Leadenhall-Lane|12/11/1990")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show.url
    AuthStubs.verifyAuthorise()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()
    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(tdAll.agentApplicationId)
    CompaniesHouseStubs.verifyMultipleMatchCalls(lastName = lastName)
    AgentRegistrationIndividualProvidedDetailsStubs.verifyUpsert()
