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
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentEmailAddressForm
import uk.gov.hmrc.agentregistrationfrontend.model.emailverification.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.EmailVerificationStubs

class AgentEmailAddressControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/agent-details/email-address"
  private val verifyPath = "/agent-registration/apply/agent-details/verify-email-address"

  private object ExpectedStrings:

    private val heading = "What email address should we use for your agent services account?"
    val documentTitle = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $documentTitle"
    val requiredError = "Error: Enter the email address for your agent services account"
    val patternError = "Error: Enter an email address in the correct format, like name@example.com"
    val tooLongError = "Error: The email address must be 132 characters or fewer"

  private object agentApplication:

    val beforeTelephoneProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterBusinessNameProvided

    val beforeEmailAddressProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterContactTelephoneSelected

    val afterContactEmailAddressSelected: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterContactEmailAddressSelected

    val afterBprEmailAddressSelected: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterBprEmailAddressSelected

    val afterOtherEmailAddressSelected: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterOtherEmailAddressSelected

    val afterEmailAddressVerified: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterVerifiedEmailAddressSelected

  private val agentEmailVerificationRequest: VerifyEmailRequest = VerifyEmailRequest(
    credId = tdAll.credentials.providerId,
    continueUrl = s"${ISpec.thisFrontendBaseUrl}/agent-registration/apply/agent-details/verify-email-address",
    origin = "HMRC Agent Services",
    deskproServiceName = None,
    accessibilityStatementUrl = "/agent-services-account",
    email = Some(Email(
      address = tdAll.newEmailAddress,
      enterUrl = s"${ISpec.thisFrontendBaseUrl}/agent-registration/apply/agent-details/email-address"
    )),
    lang = Some("en"),
    backUrl = Some(s"${ISpec.thisFrontendBaseUrl}/agent-registration/apply/agent-details/email-address"),
    pageTitle = None
  )

  "routes should have correct paths and methods" in:
    routes.AgentEmailAddressController.show shouldBe Call(
      method = "GET",
      url = path
    )
    routes.AgentEmailAddressController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    routes.AgentEmailAddressController.submit.url shouldBe routes.AgentEmailAddressController.show.url

  s"GET $path should redirect to telephone number page when telephone number is missing" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeTelephoneProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.AgentTelephoneNumberController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should return 200 and render page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeEmailAddressProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe ExpectedStrings.documentTitle
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"GET $path when existing contact email address already chosen should return 200 and render page with previous answer filled in" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterContactEmailAddressSelected)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.documentTitle
    val radioForContact = doc.mainContent.select(s"input#${AgentEmailAddressForm.key}")
    radioForContact.attr("value") shouldBe tdAll.applicantEmailAddress.value
    radioForContact.attr("checked") shouldBe "" // checked attribute is present when selected and has no value
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"GET $path when existing BPR email address already chosen should return 200 and render page with previous answer filled in" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterBprEmailAddressSelected)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.documentTitle
    val radioForBprTelephoneNumber = doc.mainContent.select(s"input#${AgentEmailAddressForm.key}-2")
    radioForBprTelephoneNumber.attr("value") shouldBe tdAll.bprEmailAddress
    radioForBprTelephoneNumber.attr("checked") shouldBe "" // checked attribute is present when selected and has no value
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"GET $path when new email address already provided should return 200 and render page with previous answer filled in" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterEmailAddressVerified)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.documentTitle
    doc.mainContent.select(s"input#${AgentEmailAddressForm.otherKey}")
      .attr("value") shouldBe tdAll.newEmailAddress
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with a new well formed email address should save data and redirect to the verify endpoint" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeEmailAddressProvided,
      updatedApplication = agentApplication.afterOtherEmailAddressSelected
    )
    val response: WSResponse =
      post(path)(Map(
        AgentEmailAddressForm.key -> Seq(Constants.OTHER),
        AgentEmailAddressForm.otherKey -> Seq(tdAll.newEmailAddress)
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.AgentEmailAddressController.verify.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeEmailAddressProvided)
    val response: WSResponse =
      post(path)(Map(
        AgentEmailAddressForm.key -> Seq("")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(s"#${AgentEmailAddressForm.key}-error").text() shouldBe ExpectedStrings.requiredError
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with invalid characters should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeEmailAddressProvided)
    val response: WSResponse =
      post(path)(Map(
        AgentEmailAddressForm.key -> Seq(Constants.OTHER),
        AgentEmailAddressForm.otherKey -> Seq("invalid-email-address")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(
      s"#${AgentEmailAddressForm.otherKey}-error"
    ).text() shouldBe ExpectedStrings.patternError
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with more than 132 characters should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeEmailAddressProvided)
    val response: WSResponse =
      post(path)(Map(
        AgentEmailAddressForm.key -> Seq(Constants.OTHER),
        AgentEmailAddressForm.otherKey -> Seq(s"invalid@${"a".repeat(133)}.com")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(
      s"#${AgentEmailAddressForm.otherKey}-error"
    ).text() shouldBe ExpectedStrings.tooLongError
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with save for later should redirect to the saved for later page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeEmailAddressProvided)
    val response: WSResponse =
      post(path)(Map(
        AgentEmailAddressForm.key -> Seq(Constants.OTHER),
        AgentEmailAddressForm.otherKey -> Seq(s"valid@email.com"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $verifyPath with an email yet to be verified in the application should redirect to the email verification frontend" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterOtherEmailAddressSelected)

    EmailVerificationStubs.stubEmailYetToBeVerified(tdAll.credentials.providerId)
    EmailVerificationStubs.stubVerificationRequest(agentEmailVerificationRequest)
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "http://localhost:9890/response-url"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    EmailVerificationStubs.verifyEvStatusRequest(tdAll.credentials.providerId)
    EmailVerificationStubs.verifyEvRequest()

  s"GET $verifyPath with an email that is unverified in the application should redirect to the email verification frontend" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterOtherEmailAddressSelected)
    EmailVerificationStubs.stubEmailStatusUnverified(tdAll.credentials.providerId, tdAll.applicantEmailAddress)
    EmailVerificationStubs.stubVerificationRequest(agentEmailVerificationRequest)
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "http://localhost:9890/response-url"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    EmailVerificationStubs.verifyEvStatusRequest(tdAll.credentials.providerId)
    EmailVerificationStubs.verifyEvRequest()

  s"GET $verifyPath with an already verified email not yet stored in the application should store and redirect to check your answers" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.afterOtherEmailAddressSelected,
      updatedApplication = agentApplication.afterEmailAddressVerified
    )
    EmailVerificationStubs.stubEmailStatusVerified(
      credId = tdAll.credentials.providerId,
      emailAddress = EmailAddress(tdAll.newEmailAddress)
    )
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()
    EmailVerificationStubs.verifyEvStatusRequest(tdAll.credentials.providerId)

  s"GET $verifyPath with an already verified email stored in the application should redirect to check your answers" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterEmailAddressVerified)
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
