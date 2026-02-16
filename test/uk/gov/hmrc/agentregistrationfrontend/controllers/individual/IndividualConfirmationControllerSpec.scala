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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs

class IndividualConfirmationControllerSpec
extends ControllerSpec:

  private val linkId = tdAll.linkId
  private val agentApplication =
    tdAll
      .agentApplicationLlp
      .afterContactDetailsComplete
  private val path: String = s"/agent-registration/provide-details/confirmation/${linkId.value}"

  object individualProvidedDetails:

    val incompleteProvidedDetails: IndividualProvidedDetails =
      tdAll
        .providedDetails
        .afterApproveAgentApplication

    val completedProvidedDetails: IndividualProvidedDetails =
      tdAll
        .providedDetails
        .afterProvidedDetailsConfirmed

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.IndividualConfirmationController.show(linkId) shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should return 200 and render the confirmation page" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvidedDetails.completedProvidedDetails,
      withBpr = true
    )
    AgentRegistrationIndividualProvidedDetailsStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.asUtr,
      responseBody = tdAll.businessPartnerRecordResponse
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "You have finished this process - Apply for an agent services account - GOV.UK"

  s"GET $path with incomplete details should return 303 and redirect to check your answers page" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvidedDetails.incompleteProvidedDetails
    )
    AgentRegistrationIndividualProvidedDetailsStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.asUtr,
      responseBody = tdAll.businessPartnerRecordResponse
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location") shouldBe Some(AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url)
