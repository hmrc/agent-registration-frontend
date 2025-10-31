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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicantcontactdetails

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import arf.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.ChOfficerSelectionForms
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.CompaniesHouseStubs

class CompaniesHouseMatchingControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/applicant/member-name-match"
  private object agentApplication:

    val afterNameQueryProvided =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAMember
        .afterNameQueryProvided

    val afterOfficerChosen =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAMember
        .afterOfficerChosen

  private val lastName = tdAll.agentApplicationLlp.sectionContactDetails.whenApplicantIsAMember.lastNameQuery

  "routes should have correct paths and methods" in:
    arf.applicantcontactdetails.routes.CompaniesHouseMatchingController.show shouldBe Call(
      method = "GET",
      url = path
    )
    arf.applicantcontactdetails.routes.CompaniesHouseMatchingController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    arf.applicantcontactdetails.routes.CompaniesHouseMatchingController.submit.url shouldBe arf.applicantcontactdetails.routes.CompaniesHouseMatchingController.show.url

  s"GET $path should return 200 and render page when there is a single match" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterNameQueryProvided)
    CompaniesHouseStubs.stubSingleMatch(lastName = lastName)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Are these your details? - Apply for an agent services account - GOV.UK"

  s"GET $path should return 200 and render page when there are multiple matches" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterNameQueryProvided)
    CompaniesHouseStubs.stubMultipleMatches(lastName = lastName)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "2 records match this name - Apply for an agent services account - GOV.UK"

  s"POST $path for single match without a valid selection should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterNameQueryProvided)
    CompaniesHouseStubs.stubSingleMatch(lastName = lastName)
    val response: WSResponse =
      post(path)(Map(
        "ChOfficerSelectionFormType" -> Seq("YesNoForm")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Are these your details? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#companiesHouseOfficer-error").text() shouldBe "Error: Select yes if these are your details"

  s"POST $path for single match with valid inputs should save officer and redirect to check your answers" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterNameQueryProvided)
    CompaniesHouseStubs.stubSingleMatch(lastName = lastName)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterOfficerChosen)
    val response: WSResponse =
      post(path)(Map(
        "ChOfficerSelectionFormType" -> Seq("YesNoForm"),
        "companiesHouseOfficer" -> Seq("Yes")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe arf.applicantcontactdetails.routes.CheckYourAnswersController.show.url

  s"POST $path for multiple matches without a valid selection should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterNameQueryProvided)
    CompaniesHouseStubs.stubMultipleMatches(lastName = lastName)
    val response: WSResponse =
      post(path)(Map(
        "ChOfficerSelectionFormType" -> Seq("OfficerSelectionForm")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: 2 records match this name - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#companiesHouseOfficer-error").text() shouldBe "Error: Select the name and date of birth that matches your details"

  s"POST $path for multiple matches with valid inputs should save officer and redirect to check your answers" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterNameQueryProvided)
    CompaniesHouseStubs.stubMultipleMatches(lastName = lastName)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterOfficerChosen)
    val response: WSResponse =
      post(path)(Map(
        "ChOfficerSelectionFormType" -> Seq("OfficerSelectionForm"),
        ChOfficerSelectionForms.key -> Seq("Taylor Reed|12/11/1990")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe arf.applicantcontactdetails.routes.CheckYourAnswersController.show.url

  s"POST $path with save for later and valid selection should save data and redirect to the saved for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterNameQueryProvided)
    CompaniesHouseStubs.stubMultipleMatches(lastName = lastName)
    AgentRegistrationStubs.stubUpdateAgentApplication(
      agentApplication.afterOfficerChosen
    )
    val response: WSResponse =
      post(path)(Map(
        "ChOfficerSelectionFormType" -> Seq("OfficerSelectionForm"),
        ChOfficerSelectionForms.key -> Seq("First Last|/1/1990"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applicationRoutes.SaveForLaterController.show.url

  s"POST $path with save for later and without a valid selection should not return errors and redirect to save for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterNameQueryProvided)
    CompaniesHouseStubs.stubSingleMatch(lastName = lastName)
    val response: WSResponse =
      post(path)(Map(
        "ChOfficerSelectionFormType" -> Seq("YesNoForm"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applicationRoutes.SaveForLaterController.show.url
