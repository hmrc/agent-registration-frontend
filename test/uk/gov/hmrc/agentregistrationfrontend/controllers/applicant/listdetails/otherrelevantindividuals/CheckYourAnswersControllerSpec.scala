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

import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.AddOtherRelevantIndividualsForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/other-relevant-individuals/check-your-answers"

  val existingOtherRelevantIndividuals = List(
    tdAll.individualProvidedDetails.copy(isPersonOfControl = false),
    tdAll.individualProvidedDetails2.copy(isPersonOfControl = false),
    tdAll.individualProvidedDetails3.copy(isPersonOfControl = false)
  )

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
    AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.submit shouldBe Call(
      method = "POST",
      url = path
    )

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
        AddOtherRelevantIndividualsForm.addOtherRelevantIndividuals -> Seq("Yes")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should return 200 and render page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId,
      individuals = existingOtherRelevantIndividuals
    )

    val response: WSResponse = get(path)

    response.status shouldBe Status.OK

    val doc: Document = response.parseBodyAsJsoupDocument

    doc.title() shouldBe "Other relevant tax advisers for Test Company Name - Apply for an agent services account - GOV.UK"
    doc.select("h1").text() shouldBe "Other relevant tax advisers for Test Company Name"
    doc.select("main button.govuk-button").eachText().toArray.mkString(" ") shouldBe "Save and continue Save and come back later"
    doc.select("main .govuk-warning-text__text").text() shouldBe ""

    doc.select("main form").attr("action") shouldBe
      AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.submit.url

    doc.select("main fieldset legend").text() shouldBe "Are there any more relevant tax advisers?"
    doc.select("main #addOtherRelevantIndividuals-hint").text() shouldBe
      "We need to know everyone who has material responsibility for how tax advice is carried out, but is not an official partner."

    val radioLabels = doc.select("main .govuk-radios__item label").eachText()
    radioLabels.contains("Yes") shouldBe true
    radioLabels.contains("No") shouldBe true

    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId)

  s"GET $path hasOtherRelevantIndividuals selected No should redirect CheckYourAnswers page for all individuals (official and unofficial)" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterConfirmOtherRelevantIndividualsNo)

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header(HeaderNames.LOCATION) shouldBe Some(
      AppRoutes.apply.listdetails.CheckYourAnswersController.show.url
    )

    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path hasOtherRelevantIndividuals Not selected should redirect to ConfirmOtherRelevantIndividuals page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeConfirmOtherRelevantIndividuals)

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header(HeaderNames.LOCATION) shouldBe Some(
      AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.show.url
    )

    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path when hasOtherRelevantIndividuals selected Yes and list is empty should redirect EnterOtherIndividual page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId,
      individuals = List.empty
    )

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header(HeaderNames.LOCATION) shouldBe Some(
      AppRoutes.apply.listdetails.otherrelevantindividuals.EnterOtherRelevantIndividualController.show.url
    )

    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId)

  s"POST $path with are there any more unofficial partners? Yes and should redirect to EnterOtherRelevantIndividual page:" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId,
      individuals = existingOtherRelevantIndividuals
    )

    val response: WSResponse =
      post(path)(Map(
        AddOtherRelevantIndividualsForm.addOtherRelevantIndividuals -> Seq("Yes")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.otherrelevantindividuals.EnterOtherRelevantIndividualController.show.url

    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId)
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with are there any more unofficial partners? No and should redirect to CYA for all Partners page:" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId,
      individuals = existingOtherRelevantIndividuals
    )

    val response: WSResponse =
      post(path)(Map(
        AddOtherRelevantIndividualsForm.addOtherRelevantIndividuals -> Seq("No")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.CheckYourAnswersController.show.url

    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId)
    ApplyStubHelper.verifyConnectorsForAuthAction()
