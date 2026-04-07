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

import scala.jdk.CollectionConverters.*

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  private val getPath = "/agent-registration/apply/list-details/companies-house-officers/check-your-answers"

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
    AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show shouldBe Call(
      method = "GET",
      url = getPath
    )

  s"GET $getPath should redirect to task list when application is a sole trader" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.soleTraderInProgress)
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $getPath with one individual entered should return 200 and show the CYA page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List(tdAll.providedDetails.precreated)
    )
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() should include("You have added 1 LLP member")
    doc.mainContent.select("h1").text() should include("You have added 1 LLP member")
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)

  s"GET $getPath should show Change and Remove action links with correct hrefs for each individual" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List(tdAll.providedDetails.precreated)
    )
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    val actions = doc.mainContent.select(".hmrc-summary-list__actions a")
    val changeLink = actions.asScala.find(_.text().contains("Change"))
    val removeLink = actions.asScala.find(_.text().contains("Remove"))
    changeLink shouldBe defined
    removeLink shouldBe defined
    changeLink.fold(fail("Change link not found"))(link =>
      link.attr("href") shouldBe
        AppRoutes.apply.listdetails.incoporated.ChangeCompaniesHouseOfficerController.show(tdAll.individualProvidedDetailsId).url
    )
    removeLink.fold(fail("Remove link not found"))(link =>
      link.attr("href") shouldBe
        AppRoutes.apply.listdetails.incoporated.RemoveCompaniesHouseOfficerController.show(tdAll.individualProvidedDetailsId).url
    )
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)

  s"GET $getPath should only show Remove link for non-precreated individuals" in:
    val nonPrecreatedIndividual = tdAll.providedDetails.afterStarted
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List(nonPrecreatedIndividual)
    )
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    val actions = doc.mainContent.select(".hmrc-summary-list__actions a")
    val changeLink = actions.asScala.find(_.text().contains("Change"))
    val removeLink = actions.asScala.find(_.text().contains("Remove"))
    changeLink shouldBe None
    removeLink shouldBe defined
    removeLink.fold(fail("Remove link not found"))(link =>
      link.attr("href") shouldBe
        AppRoutes.apply.listdetails.incoporated.RemoveCompaniesHouseOfficerController.show(tdAll.individualProvidedDetailsId).url
    )
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)

  s"GET $getPath should show 'Change the number of LLP members' link" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List(tdAll.providedDetails.precreated)
    )
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.mainContent.select("p.govuk-body a").first().text() shouldBe "Change the number of LLP members"
    doc.mainContent.select("p.govuk-body a").first().attr("href") shouldBe
      AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)

  s"GET $getPath should show inset text and 'Add another' link when more individuals are needed" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List(tdAll.providedDetails.precreated)
    )
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.mainContent.select(".govuk-inset-text").text() should include("LLP member")
    val addAnotherLink = doc.mainContent.select("a.govuk-button").asScala.find(_.text().contains("Add another LLP member"))
    addAnotherLink shouldBe defined
    addAnotherLink.fold(fail("Add another link not found"))(link =>
      link.attr("href") shouldBe AppRoutes.apply.listdetails.incoporated.EnterCompaniesHouseOfficerController.show.url
    )
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)

  s"GET $getPath with no individuals entered should redirect to enter companies house officer page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.incoporated.EnterCompaniesHouseOfficerController.show.url
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)

  s"GET $getPath should return 200 and show warning when list has too many individuals" in:
    // SixOrMoreOfficers(6, 4) → totalListSize = 5. With 6 individuals, diff = -1
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    val sixIndividuals = (1 to 6).toList.map(i =>
      tdAll.providedDetails.precreated.copy(
        _id = IndividualProvidedDetailsId(s"test-id-$i"),
        individualName = IndividualName(s"Test Name $i")
      )
    )
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = sixIndividuals
    )
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() should include("You have added 6 LLP members")
    doc.mainContent.select(".govuk-warning-text__text").text() shouldBe
      "Warning You told us there are 5 LLP members. Remove 1 LLP member from the list before you continue."
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)

  s"GET $getPath should show confirm and continue when list is complete" in:
    // SixOrMoreOfficers(6, 4) → totalListSize = 5. With 5 individuals, diff = 0
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
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
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() should include("You have added 5 LLP members")
    val confirmLink = doc.mainContent.select("a.govuk-button").asScala.find(_.text().contains("Confirm and continue"))
    confirmLink shouldBe defined
    confirmLink.fold(fail("Confirm and continue link not found"))(link =>
      link.attr("href") shouldBe AppRoutes.apply.listdetails.CheckYourAnswersController.show.url
    )
    doc.mainContent.select(".govuk-warning-text").isEmpty shouldBe true
    doc.mainContent.select(".govuk-inset-text").isEmpty shouldBe true
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)

  s"GET $getPath should redirect to CompaniesHouseOfficersController when numberOfIndividuals is five or less" in:
    ApplyStubHelper.stubsForAuthAction(
      tdAll.agentApplicationLlp.afterConfirmCompaniesHouseOfficersYes
    )
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url

  s"GET $getPath should redirect to CompaniesHouseOfficersController when numberOfIndividuals is not set" in:
    ApplyStubHelper.stubsForAuthAction(
      tdAll.agentApplicationLlp.afterHmrcStandardForAgentsAgreed
    )
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url
