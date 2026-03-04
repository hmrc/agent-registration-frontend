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
      individuals = List(tdAll.individualProvidedDetails)
    )
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() should include("You have added 1 LLP member")
    doc.mainContent.select("h1").text() should include("You have added 1 LLP member")
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)

  s"GET $getPath should show Change and Remove action links for each individual" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List(tdAll.individualProvidedDetails)
    )
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    val actions = doc.mainContent.select(".hmrc-summary-list__actions a")
    actions.toString should include("Change")
    actions.toString should include("Remove")
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)

  s"GET $getPath should show 'Change the number of LLP members' link" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List(tdAll.individualProvidedDetails)
    )
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.mainContent.select("p.govuk-body a").first().text() shouldBe "Change the number of LLP members"
    doc.mainContent.select("p.govuk-body a").first().attr("href") shouldBe
      AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)

  s"GET $getPath should show inset text when more individuals are needed" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List(tdAll.individualProvidedDetails)
    )
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.mainContent.select(".govuk-inset-text").text() should include("more LLP member")
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId)

  s"GET $getPath with no individuals entered should return 200 and show the CYA page with empty list" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterNumberOfConfirmCompaniesHouseOfficers.agentApplicationId,
      individuals = List.empty
    )
    val response: WSResponse = get(getPath)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() should include("added 0")
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
