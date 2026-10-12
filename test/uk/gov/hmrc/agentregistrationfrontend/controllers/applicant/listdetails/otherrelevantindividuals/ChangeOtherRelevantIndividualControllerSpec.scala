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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.otherrelevantindividuals

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.OtherRelevantIndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class ChangeOtherRelevantIndividualControllerSpec
extends ControllerSpec:

  private val individualToChange: IndividualProvidedDetails = tdAll.providedDetails.precreated.copy(isPersonOfControl = false)

  private val individualProvidedDetailsId: IndividualProvidedDetailsId = individualToChange.individualProvidedDetailsId

  private val path: String = s"/agent-registration/apply/list-details/change-other-relevant-individual/${individualProvidedDetailsId.value}"

  private val expectedHeading: String = "What is the full name of the person?"

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

    val soleTraderInProgress: AgentApplicationSoleTrader =
      tdAll
        .agentApplicationSoleTrader
        .afterGrsDataReceived

  private def stubExistingList(application: AgentApplicationGeneralPartnership): StubMapping = AgentRegistrationStubs.stubFindIndividualsForApplication(
    agentApplicationId = application.agentApplicationId,
    individuals = List(individualToChange)
  )

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.otherrelevantindividuals.ChangeOtherRelevantIndividualController.show(individualProvidedDetailsId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.otherrelevantindividuals.ChangeOtherRelevantIndividualController.submit(individualProvidedDetailsId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.otherrelevantindividuals.ChangeOtherRelevantIndividualController.submit(individualProvidedDetailsId).url shouldBe
      AppRoutes.apply.listdetails.otherrelevantindividuals.ChangeOtherRelevantIndividualController.show(individualProvidedDetailsId).url

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
        OtherRelevantIndividualNameForm.key -> Seq("Any Name")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should return 200 and render page prefilled with the name of the individual to change" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    stubExistingList(agentApplication.afterConfirmOtherRelevantIndividualsYes)

    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe s"$expectedHeading - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"#${OtherRelevantIndividualNameForm.key}").attr("value") shouldBe individualToChange.individualName.value
    doc.mainContent.select("form").attr("action") shouldBe path
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId)

  s"GET $path should redirect to confirm page when hasOtherRelevantIndividuals is not answered yet" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeConfirmOtherRelevantIndividuals)

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should redirect to check your answers for all individuals when hasOtherRelevantIndividuals is No" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterConfirmOtherRelevantIndividualsNo)

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should redirect to check your answers when the individual is not found" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId,
      individuals = List.empty
    )

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path should redirect to check your answers when the individual is not found" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId,
      individuals = List.empty
    )

    val response: WSResponse =
      post(path)(Map(
        OtherRelevantIndividualNameForm.key -> Seq("Changed Name"),
        "submit" -> Seq("SaveAndContinue")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails(0)

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    stubExistingList(agentApplication.afterConfirmOtherRelevantIndividualsYes)

    val response: WSResponse = post(path)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe s"Error: $expectedHeading - Apply for an agent services account - GOV.UK"
    doc.mainContent
      .select(s"#${OtherRelevantIndividualNameForm.key}-error")
      .text() shouldBe "Error: Enter the full name of the person"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails(0)

  s"POST $path with invalid inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    stubExistingList(agentApplication.afterConfirmOtherRelevantIndividualsYes)

    val response: WSResponse =
      post(path)(Map(
        OtherRelevantIndividualNameForm.key -> Seq("Invalid@@Name123")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe s"Error: $expectedHeading - Apply for an agent services account - GOV.UK"
    doc.mainContent
      .select(s"#${OtherRelevantIndividualNameForm.key}-error")
      .text() shouldBe "Error: The person’s name must only include letters a to z, hyphens, apostrophes and spaces"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails(0)

  s"POST $path with save for later and valid input should update the name and redirect to save for later" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    stubExistingList(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(individualToChange.copy(individualName = IndividualName("Changed Name")))

    val response: WSResponse =
      post(path)(Map(
        OtherRelevantIndividualNameForm.key -> Seq("Changed Name"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails()

  s"POST $path with valid input should update the name and redirect to check your answers" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    stubExistingList(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(individualToChange.copy(individualName = IndividualName("Changed Name")))

    val response: WSResponse =
      post(path)(Map(
        OtherRelevantIndividualNameForm.key -> Seq("Changed Name"),
        "submit" -> Seq("SaveAndContinue")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails()
