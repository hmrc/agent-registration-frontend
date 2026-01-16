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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.applicantcontactdetails

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.EmailAddressForm
import uk.gov.hmrc.agentregistrationfrontend.model.emailverification.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.EmailVerificationStubs

class EmailAddressControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/applicant/email-address"
  private val verifyPath = "/agent-registration/apply/applicant/verify-email-address"

  private object ExpectedStrings:

    private val heading = "If we need to email you about this application, what’s the email address?"
    val title = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $heading - Apply for an agent services account - GOV.UK"
    val requiredError = "Enter your email address"
    val invalidError = "Enter your email address with a name, @ symbol and a domain name, like yourname@example.com"
    val tooLongError = "The email address must be 132 characters or fewer"

  private object agentApplication:

    val beforeTelephoneProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .afterNameDeclared

    val beforeEmailAddressProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .afterTelephoneNumberProvided

    val afterEmailAddressProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .afterEmailAddressProvided

    val afterEmailAddressVerified: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .afterEmailAddressVerified

  private val applicantEmailVerificationRequest: VerifyEmailRequest = VerifyEmailRequest(
    credId = tdAll.credentials.providerId,
    continueUrl = s"$thisFrontendBaseUrl/agent-registration/apply/applicant/verify-email-address",
    origin = "HMRC Agent Services",
    deskproServiceName = None,
    accessibilityStatementUrl = "/agent-services-account",
    email = Some(Email(
      address = tdAll.applicantEmailAddress.value,
      enterUrl = s"$thisFrontendBaseUrl/agent-registration/apply/applicant/email-address"
    )),
    lang = Some("en"),
    backUrl = None,
    pageTitle = None
  )

  "routes should have correct paths and methods" in:
    AppRoutes.apply.applicantcontactdetails.EmailAddressController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.applicantcontactdetails.EmailAddressController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.applicantcontactdetails.EmailAddressController.submit.url shouldBe AppRoutes.apply.applicantcontactdetails.EmailAddressController.show.url

  s"GET $path should return 200 and render page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeEmailAddressProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe ExpectedStrings.title
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should redirect to telephone number page when telephone number is missing" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeTelephoneProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.applicantcontactdetails.TelephoneNumberController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with well formed email address should save data and redirect to the verify endpoint" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeEmailAddressProvided,
      updatedApplication = agentApplication.afterEmailAddressProvided
    )
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq("user@test.com")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.applicantcontactdetails.EmailAddressController.verify.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeEmailAddressProvided)
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq(Constants.EMPTY_STRING)
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(s"#${EmailAddressForm.key}-error").text() shouldBe s"Error: ${ExpectedStrings.requiredError}"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with invalid characters should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeEmailAddressProvided)
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq("[[)(*%")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(s"#${EmailAddressForm.key}-error").text() shouldBe s"Error: ${ExpectedStrings.invalidError}"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with more than 132 characters should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeEmailAddressProvided)
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq(s"invalid@${"a".repeat(132)}.com")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(
      s"#${EmailAddressForm.key}-error"
    ).text() shouldBe s"Error: ${ExpectedStrings.tooLongError}"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with save for later should redirect to the saved for later page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeEmailAddressProvided)
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq("user@test.com"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with save for later and invalid inputs should not return errors and redirect to save for later page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeEmailAddressProvided)
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq("[[)(*%"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $verifyPath with an email yet to be verified should redirect to the email verification frontend" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterEmailAddressProvided)
    EmailVerificationStubs.stubEmailYetToBeVerified(tdAll.credentials.providerId)
    EmailVerificationStubs.stubVerificationRequest(applicantEmailVerificationRequest)
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe "http://localhost:9890/response-url"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    EmailVerificationStubs.verifyEvStatusRequest(tdAll.credentials.providerId)
    EmailVerificationStubs.verifyEvRequest()

  s"GET $verifyPath with an email that has not been verified should redirect to the email verification frontend" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterEmailAddressProvided)
    EmailVerificationStubs.stubEmailStatusUnverified(tdAll.credentials.providerId, tdAll.applicantEmailAddress)
    EmailVerificationStubs.stubVerificationRequest(applicantEmailVerificationRequest)
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe "http://localhost:9890/response-url"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    EmailVerificationStubs.verifyEvStatusRequest(tdAll.credentials.providerId)
    EmailVerificationStubs.verifyEvRequest()

  s"GET $verifyPath with an already verified email not yet stored in the application should redirect to check your answers" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.afterEmailAddressProvided,
      updatedApplication = agentApplication.afterEmailAddressVerified
    )
    EmailVerificationStubs.stubEmailStatusVerified(
      credId = tdAll.credentials.providerId,
      emailAddress = tdAll.applicantEmailAddress
    )
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.applicantcontactdetails.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()
    EmailVerificationStubs.verifyEvStatusRequest(tdAll.credentials.providerId)

  s"GET $verifyPath with an already verified email stored in the application should redirect to check your answers" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterEmailAddressVerified)
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.applicantcontactdetails.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
