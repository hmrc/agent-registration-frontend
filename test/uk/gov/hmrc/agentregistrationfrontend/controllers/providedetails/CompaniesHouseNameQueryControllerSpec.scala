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
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseNameQueryForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs

class CompaniesHouseNameQueryControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/provide-details/name"

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

    val afterStarted: IndividualProvidedDetails =
      tdAll
        .providedDetailsLlp
        .afterStarted

    val afterNameQueryProvided: IndividualProvidedDetails =
      tdAll
        .providedDetailsLlp
        .afterNameQueryProvided

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.CompaniesHouseNameQueryController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.CompaniesHouseNameQueryController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.CompaniesHouseNameQueryController.submit.url shouldBe AppRoutes.providedetails.CompaniesHouseNameQueryController.show.url

  s"GET $path should return 200 and render page using company name from agent application" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(memberProvidedDetails.afterStarted))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "What is your name? - Apply for an agent services account - GOV.UK"
    doc
      .mainContent
      .selectOrFail("p.govuk-body")
      .selectOnlyOneElementOrFail()
      .text() shouldBe "We’ll check this against the business records for Test Company Name on Companies House."
    AuthStubs.verifyAuthorise()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()
    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(tdAll.agentApplicationId)

  s"GET $path when query already stored should return 200 and render page with previous answers filled in" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(memberProvidedDetails.afterNameQueryProvided))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "What is your name? - Apply for an agent services account - GOV.UK"
    doc
      .mainContent
      .selectOrFail("input#firstName")
      .selectOnlyOneElementOrFail()
      .attr("value") shouldBe "Jane"
    doc
      .mainContent
      .selectOrFail("input#lastName")
      .selectOnlyOneElementOrFail()
      .attr("value") shouldBe "Leadenhall-Lane"
    AuthStubs.verifyAuthorise()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()
    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(tdAll.agentApplicationId)

  s"POST $path with first and last names should save data and redirect to the show name matches page" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(memberProvidedDetails.afterStarted))
    AgentRegistrationIndividualProvidedDetailsStubs.stubUpsertIndividualProvidedDetails(memberProvidedDetails.afterNameQueryProvided)
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)
    val response: WSResponse =
      post(path)(Map(
        CompaniesHouseNameQueryForm.firstNameKey -> Seq("Jane"),
        CompaniesHouseNameQueryForm.lastNameKey -> Seq("Leadenhall-Lane")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.providedetails.CompaniesHouseMatchingController.show.url
    AuthStubs.verifyAuthorise()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyUpsert()

  s"POST $path with blank inputs should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(memberProvidedDetails.afterStarted))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)
    val response: WSResponse =
      post(path)(Map(
        CompaniesHouseNameQueryForm.firstNameKey -> Seq(""),
        CompaniesHouseNameQueryForm.lastNameKey -> Seq("")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your name? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#firstName-error").text() shouldBe "Error: Enter your first name"
    doc.mainContent.select("#lastName-error").text() shouldBe "Error: Enter your last name"
    AuthStubs.verifyAuthorise()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()
    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(tdAll.agentApplicationId)

  s"POST $path with invalid inputs should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(memberProvidedDetails.afterStarted))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)
    val response: WSResponse =
      post(path)(Map(
        CompaniesHouseNameQueryForm.firstNameKey -> Seq("()))"),
        CompaniesHouseNameQueryForm.lastNameKey -> Seq(";[[[")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your name? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#firstName-error").text() shouldBe "Error: Your first name must only include letters a to z, hyphens, apostrophes and spaces"
    doc.mainContent.select("#lastName-error").text() shouldBe "Error: Your last name must only include letters a to z, hyphens, apostrophes and spaces"
    AuthStubs.verifyAuthorise()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()
    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(tdAll.agentApplicationId)
