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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual.internal

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.ProvideDetailsStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs

class UcrIndividualControllerSpec
extends ControllerSpec:

  private val linkId = tdAll.linkId

  private val agentApplication =
    tdAll
      .agentApplicationLlp
      .afterContactDetailsComplete

  // Use afterSaUtrFromAuth as base — has FromAuth NINO and SA-UTR, both verified and suitable for UCR lookup
  private val beforeUcrLookup: IndividualProvidedDetails = tdAll.providedDetails.AfterSaUtr.afterSaUtrFromAuth

  private object individualProvidedDetails:

    val afterUcrProvided: IndividualProvidedDetails = beforeUcrLookup
      .copy(
        vrns = Some(List(tdAll.vrn)),
        payeRefs = Some(List(tdAll.payeRef))
      )

    val afterUcrEmpty: IndividualProvidedDetails = beforeUcrLookup
      .copy(
        vrns = Some(List.empty),
        payeRefs = Some(List.empty)
      )

  private val path: String = s"/agent-registration/provide-details/internal/unified-customer-registry-identifiers/${linkId.value}"
  private val nextPageUrl: String = s"/agent-registration/provide-details/check-your-answers/${linkId.value}"

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.internal.UcrIndividualController.populateIndividualIdentifiersFromUcr(linkId) shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should update individual provided details with vrns and payeRefs if present and redirect to check your answers" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      beforeUcrLookup
    )
    AgentRegistrationStubs.stubGetIndividualIdentifiersByNino(tdAll.nino, tdAll.ucrIdentifiers)
    AgentRegistrationIndividualProvidedDetailsStubs.stubUpsertIndividualProvidedDetails(individualProvidedDetails.afterUcrProvided)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()
    AgentRegistrationStubs.verifyGetIndividualIdentifiersByNino(tdAll.nino)
    AgentRegistrationIndividualProvidedDetailsStubs.verifyUpsertIndividualProvidedDetails()

  s"GET $path should update individual provided details with empty lists if no identifiers found and redirect to check your answers" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      beforeUcrLookup
    )
    AgentRegistrationStubs.stubGetIndividualIdentifiersByNino(tdAll.nino, tdAll.emptyUcrIdentifiers)
    AgentRegistrationIndividualProvidedDetailsStubs.stubUpsertIndividualProvidedDetails(individualProvidedDetails.afterUcrEmpty)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()
    AgentRegistrationStubs.verifyGetIndividualIdentifiersByNino(tdAll.nino)
    AgentRegistrationIndividualProvidedDetailsStubs.verifyUpsertIndividualProvidedDetails()

  s"GET $path should update individual provided details with empty lists if connector fails and redirect to check your answers" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      beforeUcrLookup
    )
    AgentRegistrationStubs.stubGetIndividualIdentifiersByNinoFails(tdAll.nino)
    AgentRegistrationIndividualProvidedDetailsStubs.stubUpsertIndividualProvidedDetails(individualProvidedDetails.afterUcrEmpty)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyUpsertIndividualProvidedDetails()

  s"GET $path should redirect to check your answers when ucr identifiers already populated" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvidedDetails.afterUcrProvided
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyUpsertIndividualProvidedDetails(0)
