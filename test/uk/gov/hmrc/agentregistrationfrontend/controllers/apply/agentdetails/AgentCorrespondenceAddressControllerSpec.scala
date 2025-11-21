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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.agentdetails

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentCorrespondenceAddressForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AddressLookupFrontendStubs

class AgentCorrespondenceAddressControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/agent-details/correspondence-address"

  private object ExpectedStrings:

    private val heading = "What correspondence address should we use for your agent services account?"
    val documentTitle = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $documentTitle"
    val requiredError = "Error: Enter the correspondence address you want to use on your agent services account"

  object agentApplication:

    val beforeEmailAddressProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterContactTelephoneSelected

    val afterEmailAddressSelected: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterVerifiedEmailAddressSelected

    val afterChroAddressSelected: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterChroAddressSelected

    val afterBprAddressSelected: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterBprAddressSelected

    val afterOtherAddressProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterOtherAddressProvided

  "routes should have correct paths and methods" in:
    routes.AgentCorrespondenceAddressController.show shouldBe Call(
      method = "GET",
      url = path
    )
    routes.AgentCorrespondenceAddressController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    routes.AgentCorrespondenceAddressController.submit.url shouldBe routes.AgentCorrespondenceAddressController.show.url

  s"GET $path before email address has been selected should redirect to the email address page" in:
    AgentDetailsStubHelper.stubsForAuthAction(agentApplication.beforeEmailAddressProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.AgentEmailAddressController.show.url
    AgentDetailsStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should return 200, fetch the BPR and render page" in:
    AgentDetailsStubHelper.stubsToRenderPage(agentApplication.afterEmailAddressSelected)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe ExpectedStrings.documentTitle
    AgentDetailsStubHelper.verifyConnectorsToRenderPage()

  s"GET $path when existing CHRO address already chosen should return 200 and render page with previous answer filled in" in:
    AgentDetailsStubHelper.stubsToRenderPage(agentApplication.afterChroAddressSelected)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.documentTitle
    val radioForChro = doc.mainContent.select(s"input#${AgentCorrespondenceAddressForm.key}") // the first radio button
    radioForChro.attr("value") shouldBe tdAll.chroAddress.toValueString
    radioForChro.attr("checked") shouldBe "" // checked attribute is present when selected and has no value
    AgentDetailsStubHelper.verifyConnectorsToRenderPage()

  s"GET $path when existing BPR address already chosen should return 200 and render page with previous answer filled in" in:
    AgentDetailsStubHelper.stubsToRenderPage(agentApplication.afterBprAddressSelected)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.documentTitle
    val radioForBprAddress = doc.mainContent.select(s"input#${AgentCorrespondenceAddressForm.key}-2") // the second radio button
    radioForBprAddress.attr("value") shouldBe tdAll.bprRegisteredAddress.toValueString
    radioForBprAddress.attr("checked") shouldBe "" // checked attribute is present when selected and has no value
    AgentDetailsStubHelper.verifyConnectorsToRenderPage()

  s"GET $path when new address already provided via ALF should return 200 and render page with previous answer filled in" in:
    AgentDetailsStubHelper.stubsToRenderPage(agentApplication.afterOtherAddressProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.documentTitle
    val radioForOtherAddress = doc.mainContent.select(s"input#${AgentCorrespondenceAddressForm.key}-4") // third radio button indexed 4 as the radio divider is the third radio item
    radioForOtherAddress.attr("value") shouldBe "other" // we do not replay the ALF provided address value in the radio button
    radioForOtherAddress.attr("checked") shouldBe "" // checked attribute is present when selected and has no value
    AgentDetailsStubHelper.verifyConnectorsToRenderPage()

  s"POST $path with selection of BPR address should save data and redirect to CYA page" in:
    AgentDetailsStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.afterEmailAddressSelected,
      updatedApplication = agentApplication.afterBprAddressSelected
    )
    val response: WSResponse =
      post(path)(Map(
        AgentCorrespondenceAddressForm.key -> Seq(tdAll.bprRegisteredAddress.toValueString)
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url
    AgentDetailsStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with selection of other should redirect to Address Lookup Frontend" in:
    AgentDetailsStubHelper.stubsForAuthAction(agentApplication.afterEmailAddressSelected)
    AddressLookupFrontendStubs.stubAddressLookupInit(
      continueUrl = "http://localhost:22201/agent-registration/apply/internal/address-lookup/journey-callback"
    )
    val response: WSResponse =
      post(path)(Map(
        AgentCorrespondenceAddressForm.key -> Seq("other")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "http://localhost:22201/agent-registration/apply/agent-details/address-lookup-response"
    AgentDetailsStubHelper.verifyConnectorsForAuthAction()
    AddressLookupFrontendStubs.verifyAddressLookupInit()

  s"POST $path with blank inputs should return 400" in:
    AgentDetailsStubHelper.stubsToRenderPage(agentApplication.afterEmailAddressSelected)
    val response: WSResponse =
      post(path)(Map(
        AgentCorrespondenceAddressForm.key -> Seq("")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(
      s"#${AgentCorrespondenceAddressForm.key}-error"
    ).text() shouldBe ExpectedStrings.requiredError
    AgentDetailsStubHelper.verifyConnectorsToRenderPage()

  s"POST $path with save for later and valid selection should save data and redirect to the saved for later page" in:
    AgentDetailsStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.afterEmailAddressSelected,
      updatedApplication = agentApplication.afterBprAddressSelected
    )
    val response: WSResponse =
      post(path)(Map(
        AgentCorrespondenceAddressForm.key -> Seq(tdAll.bprRegisteredAddress.toValueString),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    AgentDetailsStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with save for later and invalid inputs should not return errors and redirect to save for later page" in:
    AgentDetailsStubHelper.stubsToRenderPage(agentApplication.afterEmailAddressSelected)
    val response: WSResponse =
      post(path)(Map(
        AgentCorrespondenceAddressForm.key -> Seq(""),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    AgentDetailsStubHelper.verifyConnectorsToRenderPage()
