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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
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

  private val headingFirst: String = "Tell us about the LLP members who are relevant individuals"

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

  s"GET $getPath when list is already complete should redirect to CYA" in:
    // SixOrMoreOfficers(6, 4) → totalListSize = 5. With 5 individuals, list is complete.
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    val fiveIndividuals = (1 to 5).toList.map(i =>
      tdAll.providedDetails.precreated.copy(
        _id = IndividualProvidedDetailsId(s"test-id-$i"),
        individualName = IndividualName(s"Test Name $i")
      )
    )
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = fiveIndividuals
    )
    CompaniesHouseStubs.stubSixOfficers()
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  s"GET $getPath should redirect to CYA when numberOfIndividuals is five or less" in:
    ApplyStubHelper.stubsForAuthAction(
      tdAll.agentApplicationLlp.afterConfirmCompaniesHouseOfficersYes
    )
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = tdAll.agentApplicationLlp.afterConfirmCompaniesHouseOfficersYes.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubSixOfficers()
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url

  s"GET $getPath should redirect to CompaniesHouseOfficersController when numberOfIndividuals is not set" in:
    ApplyStubHelper.stubsForAuthAction(
      tdAll.agentApplicationLlp.afterHmrcStandardForAgentsAgreed
    )
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = tdAll.agentApplicationLlp.afterHmrcStandardForAgentsAgreed.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubSixOfficers()
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url

  s"GET $getPath with some individuals already entered should return 200 and render the next individual name page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List(tdAll.providedDetails.precreated)
    )
    CompaniesHouseStubs.stubSixOfficers()
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.mainContent.select("h1").text() should not be headingFirst
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  // stubSixOfficers returns 6 active officers (normalised): John Tester, John Ian Tester, Alice Tester, Bob Tester, Carol Tester, Carol Tester (duplicate)
  private def individualWithName(
    name: String,
    idSuffix: String
  ): IndividualProvidedDetails = tdAll.providedDetails.precreated.copy(
    _id = IndividualProvidedDetailsId(s"test-id-$idSuffix"),
    individualName = IndividualName(name)
  )

  s"POST $postPath should redirect to CYA when all CH officers are already in individuals list" in:
    // All 6 CH officers consumed (including both Carol Testers). Filtered list is empty.
    // List is over-complete (6 > totalListSize 5), so renderPage redirects to CYA.
    val allOfficers = List(
      individualWithName("John Tester", "1"),
      individualWithName("John Ian Tester", "2"),
      individualWithName("Alice Tester", "3"),
      individualWithName("Bob Tester", "4"),
      individualWithName("Carol Tester", "5"),
      individualWithName("Carol Tester", "6")
    )
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = allOfficers
    )
    CompaniesHouseStubs.stubSixOfficers()

    val response: WSResponse =
      post(postPath)(Map(
        CompaniesHouseIndividuaNameForm.firstNameKey -> Seq("Alice"),
        CompaniesHouseIndividuaNameForm.lastNameKey -> Seq("Tester")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url

  s"POST $postPath with duplicate CH name should still allow submission when not all occurrences are used" in:
    // "Carol Tester" appears twice in CH. With one already in individuals list, one should remain available.
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List(individualWithName("Carol Tester", "1"))
    )
    AgentRegistrationStubs.stubFindIndividualByPersonReferenceNoContent(tdAll.personReference)
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
      individualProvidedDetails = tdAll.providedDetails.precreated.copy(individualName = IndividualName("Carol Tester"))
    )
    CompaniesHouseStubs.stubSixOfficers()

    val response: WSResponse =
      post(postPath)(Map(
        CompaniesHouseIndividuaNameForm.firstNameKey -> Seq("Carol"),
        CompaniesHouseIndividuaNameForm.lastNameKey -> Seq("Tester")
      ))

    response.status shouldBe Status.SEE_OTHER
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $postPath with duplicate CH name should return 400 when all occurrences are used" in:
    // "Carol Tester" appears twice in CH. With both already in individuals list, none should remain.
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List(
        individualWithName("Carol Tester", "1"),
        individualWithName("Carol Tester", "2")
      )
    )
    CompaniesHouseStubs.stubSixOfficers()

    val response: WSResponse =
      post(postPath)(Map(
        CompaniesHouseIndividuaNameForm.firstNameKey -> Seq("Carol"),
        CompaniesHouseIndividuaNameForm.lastNameKey -> Seq("Tester")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() should startWith("Error:")
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $postPath should store the matched CH officer name, not the user-entered name" in:
    // User enters "john" / "tester" (lowercase), but CH has "John Tester".
    // The stored name should be the CH officer name "John Tester", not "john tester".
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    AgentRegistrationStubs.stubFindIndividualByPersonReferenceNoContent(tdAll.personReference)
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
      individualProvidedDetails = tdAll.providedDetails.precreated.copy(individualName = IndividualName("John Tester"))
    )
    CompaniesHouseStubs.stubSixOfficers()

    val response: WSResponse =
      post(postPath)(Map(
        CompaniesHouseIndividuaNameForm.firstNameKey -> Seq("john"),
        CompaniesHouseIndividuaNameForm.lastNameKey -> Seq("tester")
      ))

    response.status shouldBe Status.SEE_OTHER
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails()

  s"POST $postPath with valid name matching a Companies House officer should save and redirect" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    AgentRegistrationStubs.stubFindIndividualByPersonReferenceNoContent(tdAll.personReference)
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
      individualProvidedDetails = tdAll.providedDetails.precreated.copy(individualName = IndividualName("John Tester"))
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

  s"POST $postPath with save for later and valid matching name should redirect to save for later" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    AgentRegistrationStubs.stubFindIndividualByPersonReferenceNoContent(tdAll.personReference)
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
      individualProvidedDetails = tdAll.providedDetails.precreated.copy(individualName = IndividualName("John Tester"))
    )
    CompaniesHouseStubs.stubSixOfficers()

    val response: WSResponse =
      post(postPath)(Map(
        CompaniesHouseIndividuaNameForm.firstNameKey -> Seq("John"),
        CompaniesHouseIndividuaNameForm.lastNameKey -> Seq("Tester"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url

  s"POST $postPath with save for later and valid non-matching name should redirect to save for later" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubSixOfficers()

    val response: WSResponse =
      post(postPath)(Map(
        CompaniesHouseIndividuaNameForm.firstNameKey -> Seq("Unknown"),
        CompaniesHouseIndividuaNameForm.lastNameKey -> Seq("Person"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url

  s"POST $postPath with save for later and empty inputs should redirect to save for later" in:
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
