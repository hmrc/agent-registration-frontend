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
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.ConfirmOtherRelevantIndividualsForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class ConfirmOtherRelevantIndividualsControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/how-many-other-individuals"

  private val heading = "Other people we need to know about"

  object agentApplication:

    val beforeConfirmOtherRelevantIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

    val afterConfirmOtherRelevantIndividualsYes: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterConfirmOtherRelevantIndividualsYes

    val afterConfirmOtherRelevantIndividualsNo: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterConfirmOtherRelevantIndividualsNo

    val soleTraderInProgress =
      tdAll
        .agentApplicationSoleTrader
        .afterGrsDataReceived

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.submit.url shouldBe
      AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.show.url

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
        ConfirmOtherRelevantIndividualsForm.hasOtherRelevantIndividuals -> Seq("Yes")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should return 200, fetch the BPR and render page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeConfirmOtherRelevantIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeConfirmOtherRelevantIndividuals.agentApplicationId,
      individuals = List.empty
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeConfirmOtherRelevantIndividuals.agentApplicationId)

  s"POST $path with valid selection should save data and redirect to other relevant individuals CYA" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeConfirmOtherRelevantIndividuals,
      updatedApplication = agentApplication.afterConfirmOtherRelevantIndividualsYes
    )
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeConfirmOtherRelevantIndividuals.agentApplicationId,
      individuals = List.empty
    )

    val response: WSResponse =
      post(path)(Map(
        ConfirmOtherRelevantIndividualsForm.hasOtherRelevantIndividuals -> Seq("Yes")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe
      AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeConfirmOtherRelevantIndividuals.agentApplicationId)

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeConfirmOtherRelevantIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeConfirmOtherRelevantIndividuals.agentApplicationId,
      individuals = List.empty
    )

    val response: WSResponse = post(path)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe s"Error: $heading - Apply for an agent services account - GOV.UK"
    doc.mainContent
      .select(s"#${ConfirmOtherRelevantIndividualsForm.hasOtherRelevantIndividuals}-error")
      .text() shouldBe "Error: Select yes if there are any other relevant tax advisers"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeConfirmOtherRelevantIndividuals.agentApplicationId)

  s"POST $path with save for later and valid selection should save data and redirect to the saved for later page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeConfirmOtherRelevantIndividuals,
      updatedApplication = agentApplication.afterConfirmOtherRelevantIndividualsYes
    )
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeConfirmOtherRelevantIndividuals.agentApplicationId,
      individuals = List.empty
    )

    val response: WSResponse =
      post(path)(Map(
        ConfirmOtherRelevantIndividualsForm.hasOtherRelevantIndividuals -> Seq("Yes"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeConfirmOtherRelevantIndividuals.agentApplicationId)

  s"POST $path with save for later and invalid inputs should not return errors and redirect to save for later page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeConfirmOtherRelevantIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeConfirmOtherRelevantIndividuals.agentApplicationId,
      individuals = List.empty
    )

    val response: WSResponse =
      post(path)(Map(
        ConfirmOtherRelevantIndividualsForm.hasOtherRelevantIndividuals -> Seq("NOT_A_REAL_VALUE"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeConfirmOtherRelevantIndividuals.agentApplicationId)

  s"POST $path with save for later and No selection should delete other relevant individuals and save data and redirect to the saved for later page" in:
    val existingOtherRelevantIndividuals = List(
      tdAll.individualProvidedDetails.copy(isPersonOfControl = false),
      tdAll.individualProvidedDetails2.copy(isPersonOfControl = false),
      tdAll.individualProvidedDetails3.copy(isPersonOfControl = false)
    )

    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeConfirmOtherRelevantIndividuals,
      updatedApplication = agentApplication.afterConfirmOtherRelevantIndividualsNo
    )
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterConfirmOtherRelevantIndividualsNo.agentApplicationId,
      individuals = existingOtherRelevantIndividuals
    )

    existingOtherRelevantIndividuals.foreach { i =>
      AgentRegistrationStubs.stubDeleteIndividualProvidedDetails(i.individualProvidedDetailsId)
    }

    val response: WSResponse =
      post(path)(Map(
        ConfirmOtherRelevantIndividualsForm.hasOtherRelevantIndividuals -> Seq("No"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterConfirmOtherRelevantIndividualsNo.agentApplicationId)

    existingOtherRelevantIndividuals.foreach { i =>
      AgentRegistrationStubs.verifyDeleteIndividualProvidedDetails(i.individualProvidedDetailsId)
    }
