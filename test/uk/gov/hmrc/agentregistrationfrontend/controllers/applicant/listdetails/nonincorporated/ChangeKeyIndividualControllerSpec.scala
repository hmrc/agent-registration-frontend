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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.nonincorporated

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class ChangeKeyIndividualControllerSpec
extends ControllerSpec:

  private val individualToChange: IndividualProvidedDetails = tdAll.providedDetails.precreated

  private val individualProvidedDetailsId: IndividualProvidedDetailsId = individualToChange.individualProvidedDetailsId

  private val path: String = s"/agent-registration/apply/list-details/change-key-individual/${individualProvidedDetailsId.value}"

  object agentApplication:

    val beforeHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHmrcStandardForAgentsAgreed

    // FiveOrLess with 3 key individuals
    val afterHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

    // FiveOrLess with 1 key individual
    val afterOnlyOneKeyIndividual: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterOnlyOneKeyIndividual

    // SixOrMore with 3 key individuals responsible for tax matters
    val afterHowManyKeyIndividualsNeedsPadding: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividualsNeedsPadding

    val llp: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterHmrcStandardForAgentsAgreed

    val soleTrader: AgentApplicationSoleTrader =
      tdAll
        .agentApplicationSoleTrader
        .afterHmrcStandardForAgentsAgreed

  private def stubExistingList(application: AgentApplicationGeneralPartnership): StubMapping = AgentRegistrationStubs.stubFindIndividualsForApplication(
    agentApplicationId = application.agentApplicationId,
    individuals = List(individualToChange)
  )

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.nonincorporated.ChangeKeyIndividualController.show(individualProvidedDetailsId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.nonincorporated.ChangeKeyIndividualController.submit(individualProvidedDetailsId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.nonincorporated.ChangeKeyIndividualController.submit(individualProvidedDetailsId).url shouldBe
      AppRoutes.apply.listdetails.nonincorporated.ChangeKeyIndividualController.show(individualProvidedDetailsId).url

  s"GET $path should return 200 and render page prefilled with the name of the partner to change" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    stubExistingList(agentApplication.afterHowManyKeyIndividuals)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "What is the full name of the next partner? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"#${IndividualNameForm.key}").attr("value") shouldBe individualToChange.individualName.value
    doc.mainContent.select("form").attr("action") shouldBe path
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterHowManyKeyIndividuals.agentApplicationId)

  s"GET $path should return 200 and render page for the only partner when one key individual is required" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterOnlyOneKeyIndividual)
    stubExistingList(agentApplication.afterOnlyOneKeyIndividual)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "What is the full name of the partner? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"#${IndividualNameForm.key}").attr("value") shouldBe individualToChange.individualName.value
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should return 200 and render the simple page without fetching the BPR when six or more key individuals are declared" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividualsNeedsPadding)
    stubExistingList(agentApplication.afterHowManyKeyIndividualsNeedsPadding)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "What is the full name of the next partner? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"#${IndividualNameForm.key}").attr("value") shouldBe individualToChange.individualName.value
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(agentApplication.afterHowManyKeyIndividualsNeedsPadding.getUtr, count = 0)

  s"GET $path should redirect to number of key individuals page when it has not been answered" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeHowManyKeyIndividuals)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.nonincorporated.NumberOfKeyIndividualsController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should redirect to task list for incorporated businesses" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.llp)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should redirect to task list for sole traders" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.soleTrader)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    stubExistingList(agentApplication.afterHowManyKeyIndividuals)
    val response: WSResponse = post(path)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is the full name of the next partner? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${IndividualNameForm.key}-error"
    ).text() shouldBe "Error: Enter the full name of the partner"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails(0)

  s"POST $path with invalid inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    stubExistingList(agentApplication.afterHowManyKeyIndividuals)
    val response: WSResponse =
      post(path)(Map(
        IndividualNameForm.key -> Seq("Invalid@@Name123")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is the full name of the next partner? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${IndividualNameForm.key}-error"
    ).text() shouldBe "Error: The partner’s name must only include letters a to z, hyphens, apostrophes and spaces"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails(0)

  s"POST $path with save for later and valid input should update the name and redirect to save for later" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    stubExistingList(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(individualToChange.copy(individualName = IndividualName("Changed Name")))

    val response: WSResponse =
      post(path)(Map(
        IndividualNameForm.key -> Seq("Changed Name"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails()

  s"POST $path with valid input should update the name and redirect to check your answers" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    stubExistingList(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(individualToChange.copy(individualName = IndividualName("Changed Name")))

    val response: WSResponse =
      post(path)(Map(
        IndividualNameForm.key -> Seq("Changed Name"),
        "submit" -> Seq("SaveAndContinue")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails()
