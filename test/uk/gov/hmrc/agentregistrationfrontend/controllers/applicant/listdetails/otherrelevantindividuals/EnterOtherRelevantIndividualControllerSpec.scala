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

import play.api.http.HeaderNames
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class EnterOtherRelevantIndividualControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/enter-other-relevant-individual"

  // In ControllerSpec we don't have the same `messages(...)` helper as ViewSpecs.
  // Keep this aligned with the rendered page title.
  private val expectedHeading = "What is the full name of an unofficial partner?"

  object agentApplication:

    val beforeConfirmOtherRelevantIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

    val afterConfirmOtherRelevantIndividualsYes: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterConfirmOtherRelevantIndividualsYes

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.otherrelevantindividuals.EnterOtherRelevantIndividualController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.otherrelevantindividuals.EnterOtherRelevantIndividualController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.otherrelevantindividuals.EnterOtherRelevantIndividualController.submit.url shouldBe
      AppRoutes.apply.listdetails.otherrelevantindividuals.EnterOtherRelevantIndividualController.show.url

  s"GET $path should return 200 and render the enter name page when hasOtherRelevantIndividuals is true" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId,
      individuals = List.empty
    )

    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe s"$expectedHeading - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId)

  s"GET $path should redirect to confirm page when hasOtherRelevantIndividuals is not answered yet" in:
    // The controller redirects before it tries to fetch individuals, so we should NOT stub/verify that call here.
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeConfirmOtherRelevantIndividuals)

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header(HeaderNames.LOCATION).value shouldBe
      AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId,
      individuals = List.empty
    )

    val response: WSResponse = post(path)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe s"Error: $expectedHeading - Apply for an agent services account - GOV.UK"
    doc.mainContent
      .select(s"#${IndividualNameForm.key}-error")
      .text() shouldBe "Error: Enter the full name of the partner"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId)

  s"POST $path with invalid inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterConfirmOtherRelevantIndividualsYes)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId,
      individuals = List.empty
    )

    val response: WSResponse =
      post(path)(Map(
        IndividualNameForm.key -> Seq("Invalid@@Name123")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe s"Error: $expectedHeading - Apply for an agent services account - GOV.UK"
    doc.mainContent
      .select(s"#${IndividualNameForm.key}-error")
      .text() shouldBe "Error: The partner’s name must only include letters a to z, hyphens, apostrophes and spaces"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterConfirmOtherRelevantIndividualsYes.agentApplicationId)
