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

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.RemoveKeyIndividualForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs

class RemoveOtherRelevantIndividualControllerSpec
extends ControllerSpec:

  private val individualProvidedDetailsId: IndividualProvidedDetailsId = tdAll.individualProvidedDetails.individualProvidedDetailsId

  private val path = s"/agent-registration/apply/list-details/remove-other-relevant-individual/${individualProvidedDetailsId.value}"

  object agentApplication:

    val afterHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

    val soleTraderInProgress =
      tdAll
        .agentApplicationSoleTrader
        .afterGrsDataReceived

  val listOfTwoIndividualProvidedDetails: List[IndividualProvidedDetails] = List(
    tdAll.individualProvidedDetails.copy(isPersonOfControl = false),
    tdAll.individualProvidedDetails2.copy(isPersonOfControl = false)
  )

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.otherrelevantindividuals.RemoveOtherRelevantIndividualController
      .show(individualProvidedDetailsId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.otherrelevantindividuals.RemoveOtherRelevantIndividualController
      .submit(individualProvidedDetailsId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.otherrelevantindividuals.RemoveOtherRelevantIndividualController
      .submit(individualProvidedDetailsId)
      .url shouldBe
      AppRoutes.apply.listdetails.otherrelevantindividuals.RemoveOtherRelevantIndividualController
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
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.soleTraderInProgress.agentApplicationId,
      individuals = listOfTwoIndividualProvidedDetails
    )
    val response: WSResponse =
      post(path)(Map(
        RemoveKeyIndividualForm.key -> Seq("Yes")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should return 200 and render page for removing selected unofficial partner" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHowManyKeyIndividuals.agentApplicationId,
      individuals = listOfTwoIndividualProvidedDetails
    )

    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument
      .title() shouldBe "Confirm that you want to remove Test Name from the list of partners - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHowManyKeyIndividuals.agentApplicationId,
      individuals = listOfTwoIndividualProvidedDetails
    )

    val response: WSResponse = post(path)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Confirm that you want to remove Test Name from the list of partners - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${RemoveKeyIndividualForm.key}-error"
    ).text() shouldBe "Error: Select yes if you want to remove Test Name from the list of partners"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with valid inputs should redirect to check your answers page when there are more than 1 other relevant individuals in the list before deletion" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHowManyKeyIndividuals.agentApplicationId,
      individuals = listOfTwoIndividualProvidedDetails
    )
    AgentRegistrationIndividualProvidedDetailsStubs.stubDeleteIndividualProvidedDetails(individualProvidedDetailsId)

    val response: WSResponse =
      post(path)(Map(
        RemoveKeyIndividualForm.key -> Seq("Yes")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with valid inputs should redirect to confirm other relevant individuals page when there is only 1 other relevant individual in the list before deletion" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHowManyKeyIndividuals.agentApplicationId,
      individuals = List(tdAll.individualProvidedDetails.copy(isPersonOfControl = false))
    )
    AgentRegistrationIndividualProvidedDetailsStubs.stubDeleteIndividualProvidedDetails(individualProvidedDetailsId)
    val response: WSResponse =
      post(path)(Map(
        RemoveKeyIndividualForm.key -> Seq("Yes")
      ))
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
