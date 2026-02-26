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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.incorporated

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistrationfrontend.forms.ConfirmCompaniesHouseOfficersForm
import uk.gov.hmrc.agentregistrationfrontend.forms.NumberCompaniesHouseOfficersForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.CompaniesHouseStubs
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficerRole
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class CompaniesHouseOfficersControllerSpec
extends ControllerSpec:

  private val getPath = "/agent-registration/apply/list-details/companies-house-officers"
  private val postFiveOrLessPath = "/agent-registration/apply/list-details/confirm-companies-house-officers"
  private val postSixOrMorePath = "/agent-registration/apply/list-details/how-many-companies-house-officers"

  private val headingFiveOrLess: String = s"Check this list of members for ${tdAll.companyName}"
  private val headingSixOrMore: String = s"Members responsible for tax activities at ${tdAll.companyName}"

  object agentApplication:

    val beforeCompaniesHouseOfficers: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterHmrcStandardForAgentsAgreed

    val afterFiveOrLessCompaniesHouseOfficersYes: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterConfirmCompaniesHouseOfficersYes

    val afterFiveOrLessCompaniesHouseOfficersNo: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterConfirmCompaniesHouseOfficersNo

    val afterNumberOfConfirmCompaniesHouseOfficers: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterNumberOfConfirmCompaniesHouseOfficers

    val soleTraderInProgress =
      tdAll
        .agentApplicationSoleTrader
        .afterGrsDataReceived

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show shouldBe Call(
      method = "GET",
      url = getPath
    )
    AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.submitFiveOrLess shouldBe Call(
      method = "POST",
      url = postFiveOrLessPath
    )

    AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.submitSixOrMore shouldBe Call(
      method = "POST",
      url = postSixOrMorePath
    )

  s"GET $getPath should redirect to task list when application is a sole trader" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.soleTraderInProgress)
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $postFiveOrLessPath should redirect to task list when application is a sole trader" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.soleTraderInProgress)
    val response: WSResponse =
      post(postFiveOrLessPath)(Map(
        ConfirmCompaniesHouseOfficersForm.isCompaniesHouseOfficersListCorrect -> Seq("Yes")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $getPath for five or less companies house officers should return 200, fetch the BPR and Companies House and render page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubFiveOrLess(name = tdAll.individualProvidedDetails.individualName.value)
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe s"$headingFiveOrLess - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  s"GET $getPath for six or more companies house officers should return 200, fetch the BPR and Companies House and render page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubSixOfficers()
    val response: WSResponse = get(getPath)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe s"$headingSixOrMore - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  // TODO WG - confirm and update that test
  s"GET $getPath when Companies House returns no officers should display update Companies House advice page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubFiveOrLess(
      name = tdAll.individualProvidedDetails.individualName.value,
      officerRole = CompaniesHouseOfficerRole.Director
    )

    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "You need to update Companies House - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("h1").text() shouldBe "You need to update Companies House"
//    doc.mainContent.select("p").text() should include("There are no active officers recorded at Companies House for this company")
//    doc.mainContent.select("p").text() should include("Please update your Companies House record before continuing")
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  s"POST $postFiveOrLessPath with valid selection should save data and redirect to other relevant individuals CYA" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeCompaniesHouseOfficers,
      updatedApplication = agentApplication.afterFiveOrLessCompaniesHouseOfficersYes
    )
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
      individualProvidedDetails = tdAll.individualProvidedDetails
    )

    CompaniesHouseStubs.stubFiveOrLess(name = tdAll.individualProvidedDetails.individualName.value)

    val response: WSResponse =
      post(postFiveOrLessPath)(Map(
        ConfirmCompaniesHouseOfficersForm.isCompaniesHouseOfficersListCorrect -> Seq("Yes")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe
      AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  // TODO WG - change that once page is done
  s"POST $postFiveOrLessPath with 'No' selection should display update Companies House advice page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeCompaniesHouseOfficers)
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubFiveOrLess(name = tdAll.individualProvidedDetails.individualName.value)

    val response: WSResponse =
      post(postFiveOrLessPath)(Map(
        ConfirmCompaniesHouseOfficersForm.isCompaniesHouseOfficersListCorrect -> Seq("No")
      ))

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "You need to update Companies House - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("h1").text() shouldBe "You need to update Companies House"
    doc.mainContent.select("p.govuk-body").text() should include("we need a verifiable list of the current members of")
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeCompaniesHouseOfficers.agentApplicationId)

  s"POST $postSixOrMorePath with valid selection should save data and redirect to other relevant individuals CYA" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeCompaniesHouseOfficers,
      updatedApplication = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers
    )
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
      individualProvidedDetails = tdAll.individualProvidedDetails.copy(individualName = IndividualName("Tester, John"))
    )
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
      individualProvidedDetails = tdAll.individualProvidedDetails.copy(individualName = IndividualName("Tester, John Ian"))
    )
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
      individualProvidedDetails = tdAll.individualProvidedDetails.copy(individualName = IndividualName("Tester, Alice"))
    )
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
      individualProvidedDetails = tdAll.individualProvidedDetails.copy(individualName = IndividualName("Tester, Bob"))
    )
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
      individualProvidedDetails = tdAll.individualProvidedDetails.copy(individualName = IndividualName("Tester, Carol"))
    )

    CompaniesHouseStubs.stubSixOfficers()

    val response: WSResponse =
      post(postSixOrMorePath)(Map(
        NumberCompaniesHouseOfficersForm.numberOfOfficersResponsibleForTaxMatters -> Seq("4")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe
      AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls(1)

  s"POST $postFiveOrLessPath with blank inputs should return 400 and show an error" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubFiveOrLess(name = tdAll.individualProvidedDetails.individualName.value)

    val response: WSResponse = post(postFiveOrLessPath)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() should startWith("Error:")
    doc.mainContent
      .select(s"#${ConfirmCompaniesHouseOfficersForm.isCompaniesHouseOfficersListCorrect}-error")
      .text() should include("Error:")
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  s"POST $postFiveOrLessPath with invalid value should return 400 and show an error" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubFiveOrLess(name = tdAll.individualProvidedDetails.individualName.value)

    val response: WSResponse =
      post(postFiveOrLessPath)(Map(
        ConfirmCompaniesHouseOfficersForm.isCompaniesHouseOfficersListCorrect -> Seq("NOT_A_REAL_VALUE")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() should startWith("Error:")
    doc.mainContent
      .select(s"#${ConfirmCompaniesHouseOfficersForm.isCompaniesHouseOfficersListCorrect}-error")
      .text() should include("Error:")
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  s"POST $postFiveOrLessPath with save for later and valid selection should redirect to save for later" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeCompaniesHouseOfficers,
      updatedApplication = agentApplication.afterFiveOrLessCompaniesHouseOfficersYes
    )
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
      individualProvidedDetails = tdAll.individualProvidedDetails
    )
    CompaniesHouseStubs.stubFiveOrLess(name = tdAll.individualProvidedDetails.individualName.value)

    val response: WSResponse =
      post(postFiveOrLessPath)(Map(
        ConfirmCompaniesHouseOfficersForm.isCompaniesHouseOfficersListCorrect -> Seq("Yes"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  s"POST $postFiveOrLessPath with save for later and invalid inputs should still redirect to save for later (no 400)" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubFiveOrLess(name = tdAll.individualProvidedDetails.individualName.value)

    val response: WSResponse =
      post(postFiveOrLessPath)(Map(
        ConfirmCompaniesHouseOfficersForm.isCompaniesHouseOfficersListCorrect -> Seq("NOT_A_REAL_VALUE"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls()

  s"POST $postSixOrMorePath with blank inputs should return 400 and show an error" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubSixOfficers()

    val response: WSResponse = post(postSixOrMorePath)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() should startWith("Error:")
    doc.mainContent
      .select(s"#${NumberCompaniesHouseOfficersForm.numberOfOfficersResponsibleForTaxMatters}-error")
      .text() should include("Error:")
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls(1)

  s"POST $postSixOrMorePath with non-numeric input should return 400 and show an error" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    CompaniesHouseStubs.stubSixOfficers()

    val response: WSResponse =
      post(postSixOrMorePath)(Map(
        NumberCompaniesHouseOfficersForm.numberOfOfficersResponsibleForTaxMatters -> Seq("not-a-number")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() should startWith("Error:")
    doc.mainContent
      .select(s"#${NumberCompaniesHouseOfficersForm.numberOfOfficersResponsibleForTaxMatters}-error")
      .text() should include("Error:")
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeCompaniesHouseOfficers.agentApplicationId)
    CompaniesHouseStubs.verifySixOfficersCalls(1)

//  s"POST $postSixOrMorePath with save for later and valid selection should redirect to save for later" in:
//    ApplyStubHelper.stubsForSuccessfulUpdate(
//      application = agentApplication.beforeCompaniesHouseOfficers,
//      updatedApplication = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers
//    )
//    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeCompaniesHouseOfficers)
//    AgentRegistrationStubs.stubFindIndividualsForApplication(
//      agentApplicationId = agentApplication.beforeCompaniesHouseOfficers.agentApplicationId,
//      individuals = List.empty
//    )
//    CompaniesHouseStubs.stubSixOfficers()
//    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
//      individualProvidedDetails = tdAll.individualProvidedDetails.copy(individualName = IndividualName("Tester, John"))
//    )
//    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
//      individualProvidedDetails = tdAll.individualProvidedDetails.copy(individualName = IndividualName("Tester, John Ian"))
//    )
//    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
//      individualProvidedDetails = tdAll.individualProvidedDetails.copy(individualName = IndividualName("Tester, Alice"))
//    )
//    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
//      individualProvidedDetails = tdAll.individualProvidedDetails.copy(individualName = IndividualName("Tester, Bob"))
//    )
//    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
//      individualProvidedDetails = tdAll.individualProvidedDetails.copy(individualName = IndividualName("Tester, Carol"))
//    )
//
//    val response: WSResponse =
//      post(postSixOrMorePath)(Map(
//        NumberCompaniesHouseOfficersForm.numberOfOfficersResponsibleForTaxMatters -> Seq("1"),
//        "submit" -> Seq("SaveAndComeBackLater")
//      ))
//
//    response.status shouldBe Status.SEE_OTHER
//    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
//    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
//    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeCompaniesHouseOfficers.agentApplicationId)
//    CompaniesHouseStubs.verifySixOfficersCalls()
