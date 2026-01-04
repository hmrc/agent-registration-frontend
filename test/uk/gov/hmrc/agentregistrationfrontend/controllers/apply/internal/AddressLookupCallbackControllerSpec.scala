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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.internal

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.model.addresslookup.JourneyId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AddressLookupFrontendStubs

class AddressLookupCallbackControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/internal/address-lookup/journey-callback"

  object agentApplication:

    val afterEmailAddressSelected: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterVerifiedEmailAddressSelected

    val afterOtherAddressProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterOtherAddressProvided

  "routes should have correct paths and methods" in:
    AppRoutes.apply.internal.AddressLookupCallbackController.journeyCallback(None) shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path after returning from Address Lookup Frontend should save the selected address and redirect to CYA page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.afterEmailAddressSelected,
      updatedApplication = agentApplication.afterOtherAddressProvided
    )
    AddressLookupFrontendStubs.stubAddressLookupWithId(
      journeyId = JourneyId("address-id-123"),
      address = tdAll.newCorrespondenceAddress
    )
    val response: WSResponse = get(s"$path?id=${JourneyId("address-id-123").value}")

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.agentdetails.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()
    AddressLookupFrontendStubs.verifyAddressLookupWithId(JourneyId("address-id-123"))
