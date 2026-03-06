/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.incorporated

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseIndividuaNameForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.CompaniesHouseStubs

class EnterCompaniesHouseOfficerControllerSpec
extends ControllerSpec:

  private val getPath = "/agent-registration/apply/list-details/enter-companies-house-officer"
  private val postPath = "/agent-registration/apply/list-details/enter-companies-house-officer"

  private val headingFirst: String = s"Tell us about the members of ${tdAll.companyName}"

  object agentApplication:

    val afterNumberOfConfirmCompaniesHouseOfficers: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterNumberOfConfirmCompaniesHouseOfficers

    val soleTraderInProgress =
      tdAll
        .agentApplicationSoleTrader
        .afterGrsDataReceived

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.incoporated.EnterCompaniesHouseOfficerController.show shouldBe Call(
      method = "GET",
      url = getPath
    )
    AppRoutes.apply.listdetails.incoporated.EnterCompaniesHouseOfficerController.submit shouldBe Call(
      method = "POST",
      url = postPath
    )

  s"GET $getPath should redirect to task list when application is a sole trader" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.soleTraderInProgress)
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $postPath should redirect to task list when application is a sole trader" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.soleTraderInProgress)
    val response: WSResponse =
      post(postPath)(Map(
        CompaniesHouseIndividuaNameForm.firstNameKey -> Seq("John"),
        CompaniesHouseIndividuaNameForm.lastNameKey -> Seq("Tester")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $getPath with no individuals entered should return 200 and render the first individual name page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubSixOfficers()
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe s"$headingFirst - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  s"GET $getPath with some individuals already entered should return 200 and render the next individual name page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List(tdAll.individualProvidedDetails)
    )
    CompaniesHouseStubs.stubSixOfficers()
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.mainContent.select("h1").text() should not be headingFirst
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  s"POST $postPath with valid name matching a Companies House officer should save and redirect" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
      individualProvidedDetails = tdAll.individualProvidedDetails.copy(individualName = IndividualName("John Tester"))
    )
    CompaniesHouseStubs.stubSixOfficers()

    val response: WSResponse =
      post(postPath)(Map(
        CompaniesHouseIndividuaNameForm.firstNameKey -> Seq("John"),
        CompaniesHouseIndividuaNameForm.lastNameKey -> Seq("Tester")
      ))

    response.status shouldBe Status.SEE_OTHER
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  s"POST $postPath with name not matching any Companies House officer should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubSixOfficers()

    val response: WSResponse =
      post(postPath)(Map(
        CompaniesHouseIndividuaNameForm.firstNameKey -> Seq("Unknown"),
        CompaniesHouseIndividuaNameForm.lastNameKey -> Seq("Person")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() should startWith("Error:")
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  s"POST $postPath with blank inputs should return 400 and show an error" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubSixOfficers()

    val response: WSResponse = post(postPath)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() should startWith("Error:")
    doc.mainContent
      .select(s"#${CompaniesHouseIndividuaNameForm.firstNameKey}-error")
      .text() should include("Error:")
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  s"POST $postPath with save for later should redirect to save for later" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubSixOfficers()

    val response: WSResponse =
      post(postPath)(Map(
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  s"POST $postPath with save for later and invalid inputs should still redirect to save for later" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubSixOfficers()

    val response: WSResponse =
      post(postPath)(Map(
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()
