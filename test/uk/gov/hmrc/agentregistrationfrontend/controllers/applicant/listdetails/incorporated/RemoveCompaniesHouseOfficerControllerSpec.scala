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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.RemoveKeyIndividualForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class RemoveCompaniesHouseOfficerControllerSpec
extends ControllerSpec:

  private val individualProvidedDetailsId: IndividualProvidedDetailsId = tdAll.providedDetails.precreated.individualProvidedDetailsId

  private val path = s"/agent-registration/apply/list-details/remove-companies-house-officer/${individualProvidedDetailsId.value}"

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
    AppRoutes.apply.listdetails.incoporated.RemoveCompaniesHouseOfficerController
      .show(individualProvidedDetailsId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.incoporated.RemoveCompaniesHouseOfficerController
      .submit(individualProvidedDetailsId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.incoporated.RemoveCompaniesHouseOfficerController
      .submit(individualProvidedDetailsId)
      .url shouldBe
      AppRoutes.apply.listdetails.incoporated.RemoveCompaniesHouseOfficerController
        .show(individualProvidedDetailsId)
        .url

  s"GET $path should redirect to task list when application is a sole trader" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.soleTraderInProgress)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path should redirect to task list when application is a sole trader" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.soleTraderInProgress)
    val response: WSResponse =
      post(path)(Map(
        RemoveKeyIndividualForm.key -> Seq("Yes")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should return 200 and render the remove confirmation page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualForApplication(
      individual = tdAll.providedDetails.precreated
    )

    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument
      .title() should include(s"Confirm that you want to remove ${tdAll.providedDetails.precreated.individualName.value}")
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with Yes should delete the individual and redirect to CYA" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualForApplication(
      individual = tdAll.providedDetails.precreated
    )
    AgentRegistrationStubs.stubDeleteIndividualProvidedDetails(tdAll.individualProvidedDetailsId)

    val response: WSResponse =
      post(path)(Map(
        RemoveKeyIndividualForm.key -> Seq("Yes")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe
      AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyDeleteIndividualProvidedDetails(tdAll.individualProvidedDetailsId)

  s"POST $path with No should redirect to CYA without deleting" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualForApplication(
      individual = tdAll.providedDetails.precreated
    )

    val response: WSResponse =
      post(path)(Map(
        RemoveKeyIndividualForm.key -> Seq("No")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe
      AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyDeleteIndividualProvidedDetails(tdAll.individualProvidedDetailsId, count = 0)

  s"POST $path with blank inputs should return 400 and show an error" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualForApplication(
      individual = tdAll.providedDetails.precreated
    )

    val response: WSResponse = post(path)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() should startWith("Error:")
    doc.mainContent
      .select(s"#${RemoveKeyIndividualForm.key}-error")
      .text() should include("Error:")
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with save for later should redirect to save for later" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualForApplication(
      individual = tdAll.providedDetails.precreated
    )

    val response: WSResponse =
      post(path)(Map(
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with save for later and valid input should redirect to save for later" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterNumberOfConfirmCompaniesHouseOfficers)
    AgentRegistrationStubs.stubFindIndividualForApplication(
      individual = tdAll.providedDetails.precreated
    )
    AgentRegistrationStubs.stubDeleteIndividualProvidedDetails(tdAll.individualProvidedDetailsId)

    val response: WSResponse =
      post(path)(Map(
        RemoveKeyIndividualForm.key -> Seq("Yes"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
