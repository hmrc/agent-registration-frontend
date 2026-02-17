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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails

import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/check-your-answers"

  object agentApplication:

    val afterHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

    val afterConfirmOtherRelevantIndividualsYes: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterConfirmOtherRelevantIndividualsYes

    val beforeConfirmOtherRelevantIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

    val afterHowManyKeyIndividualsNeedsPadding: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividualsNeedsPadding

    val soleTraderInProgress =
      tdAll
        .agentApplicationSoleTrader
        .afterGrsDataReceived

  val completeIndividualsList = List(
    tdAll.individualProvidedDetails, // partner (isPersonOfControl = true in test data)
    tdAll.individualProvidedDetails2, // partner
    tdAll.individualProvidedDetails3, // partner
    tdAll.individualProvidedDetails.copy(isPersonOfControl = false) // other relevant individual
  )

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.CheckYourAnswersController.show shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should redirect to task list when application is a sole trader" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.soleTraderInProgress)

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header(HeaderNames.LOCATION).value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should redirect to non-incorporated CYA when partners list is incomplete" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividualsNeedsPadding)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHowManyKeyIndividualsNeedsPadding.agentApplicationId,
      individuals = List(tdAll.individualProvidedDetails) // only 1 partner => incomplete
    )

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header(HeaderNames.LOCATION) shouldBe Some(
      AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show.url
    )

    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterHowManyKeyIndividualsNeedsPadding.agentApplicationId)

  s"GET $path should redirect to other relevant individuals CYA when hasOtherRelevantIndividuals is not answered" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeConfirmOtherRelevantIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.beforeConfirmOtherRelevantIndividuals.agentApplicationId,
      individuals = List(
        tdAll.individualProvidedDetails,
        tdAll.individualProvidedDetails2,
        tdAll.individualProvidedDetails3
      )
    )

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header(HeaderNames.LOCATION) shouldBe Some(
      AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url
    )

    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.beforeConfirmOtherRelevantIndividuals.agentApplicationId)

  s"GET $path should return 200 and render page when lists are complete and hasOtherRelevantIndividuals is defined" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId,
      individuals = completeIndividualsList
    )

    val response: WSResponse = get(path)

    response.status shouldBe Status.OK

    val doc: Document = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"
    doc.select("h1").text() shouldBe "Check your answers"

    doc.select("main").text() should include("Confirm and continue")
    doc.select("main").text() should include("Save and come back later")

    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId)
