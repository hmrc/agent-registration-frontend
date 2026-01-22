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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualEmailAddressForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.EmailVerificationStubs
import uk.gov.hmrc.agentregistrationfrontend.shared.model.emailverification.Email
import uk.gov.hmrc.agentregistrationfrontend.shared.model.emailverification.VerifyEmailRequest
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class IndividualEmailAddressControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/provide-details/email-address"
  private val verifyPath = "/agent-registration/provide-details/verify-email-address"

  private object ExpectedStrings:

    private val heading = "What is your email address?"
    val title = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $heading - Apply for an agent services account - GOV.UK"
    val requiredError = "Enter your email address"
    val invalidError = "Enter your email address with a name, @ symbol and a domain name, like yourname@example.com"
    val tooLongError = "The email address must be 132 characters or fewer"

  private object individualProvidedDetails:

    val beforeTelephoneProvided: IndividualProvidedDetails = tdAll.providedDetailsLlp.afterOfficerChosen

    val beforeEmailAddressProvided: IndividualProvidedDetails = tdAll.providedDetailsLlp.afterTelephoneNumberProvided

    val afterEmailAddressProvided: IndividualProvidedDetails = tdAll.providedDetailsLlp.afterEmailAddressProvided

    val afterEmailAddressVerified: IndividualProvidedDetails = tdAll.providedDetailsLlp.afterEmailAddressVerified

  private val individualEmailVerificationRequest: VerifyEmailRequest = VerifyEmailRequest(
    credId = tdAll.credentials.providerId,
    continueUrl = s"$thisFrontendBaseUrl/agent-registration/provide-details/verify-email-address",
    origin = "HMRC Agent Services",
    deskproServiceName = None,
    accessibilityStatementUrl = "/agent-services-account",
    email = Some(Email(
      address = tdAll.individualEmailAddress.value,
      enterUrl = s"$thisFrontendBaseUrl/agent-registration/provide-details/email-address"
    )),
    lang = Some("en"),
    backUrl = None,
    pageTitle = None
  )

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.IndividualEmailAddressController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.IndividualEmailAddressController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.IndividualEmailAddressController.submit.url shouldBe AppRoutes.providedetails.IndividualEmailAddressController.show.url

  s"GET $path should return 200 and render page" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails.beforeEmailAddressProvided))
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe ExpectedStrings.title

  s"GET $path should redirect to telephone number page when telephone number is missing" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails.beforeTelephoneProvided))
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.IndividualTelephoneNumberController.show.url

  s"POST $path with well formed email address should save data and redirect to the verify endpoint" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails.beforeEmailAddressProvided))
    AgentRegistrationIndividualProvidedDetailsStubs.stubUpsertIndividualProvidedDetails(individualProvidedDetails.afterEmailAddressProvided)
    val response: WSResponse =
      post(path)(Map(
        IndividualEmailAddressForm.key -> Seq("member@test.com")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.IndividualEmailAddressController.verify.url

  s"POST $path with blank inputs should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails.beforeEmailAddressProvided))
    val response: WSResponse =
      post(path)(Map(
        IndividualEmailAddressForm.key -> Seq(Constants.EMPTY_STRING)
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(s"#${IndividualEmailAddressForm.key}-error").text() shouldBe s"Error: ${ExpectedStrings.requiredError}"

  s"POST $path with invalid characters should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails.beforeEmailAddressProvided))
    val response: WSResponse =
      post(path)(Map(
        IndividualEmailAddressForm.key -> Seq("[[)(*%")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(s"#${IndividualEmailAddressForm.key}-error").text() shouldBe s"Error: ${ExpectedStrings.invalidError}"

  s"POST $path with more than 132 characters should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails.beforeEmailAddressProvided))
    val response: WSResponse =
      post(path)(Map(
        IndividualEmailAddressForm.key -> Seq(s"invalid@${"a".repeat(132)}.com")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(
      s"#${IndividualEmailAddressForm.key}-error"
    ).text() shouldBe s"Error: ${ExpectedStrings.tooLongError}"

  s"GET $verifyPath with an email not yet verified should redirect to the email verification frontend" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails.afterEmailAddressProvided))
    EmailVerificationStubs.stubEmailYetToBeVerified(tdAll.credentials.providerId)
    EmailVerificationStubs.stubVerificationRequest(individualEmailVerificationRequest)
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe "http://localhost:9890/response-url"
    EmailVerificationStubs.verifyEvStatusRequest(tdAll.credentials.providerId)
    EmailVerificationStubs.verifyEvRequest()

  s"GET $verifyPath with an email that is unverified should redirect to the email verification frontend" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails.afterEmailAddressProvided))
    EmailVerificationStubs.stubEmailStatusUnverified(tdAll.credentials.providerId, tdAll.individualEmailAddress)
    EmailVerificationStubs.stubVerificationRequest(individualEmailVerificationRequest)
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe "http://localhost:9890/response-url"
    EmailVerificationStubs.verifyEvStatusRequest(tdAll.credentials.providerId)
    EmailVerificationStubs.verifyEvRequest()

  s"GET $verifyPath with an already verified email not yet stored in provided details should redirect to Individual Nino page" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails.afterEmailAddressProvided))
    AgentRegistrationIndividualProvidedDetailsStubs.stubUpsertIndividualProvidedDetails(individualProvidedDetails.afterEmailAddressVerified)
    EmailVerificationStubs.stubEmailStatusVerified(
      credId = tdAll.credentials.providerId,
      emailAddress = tdAll.individualEmailAddress
    )
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show.url
    EmailVerificationStubs.verifyEvStatusRequest(tdAll.credentials.providerId)

  s"GET $verifyPath with an already verified email stored in provided details should redirect to Individual Nino page" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails.afterEmailAddressVerified))
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show.url

  s"GET $verifyPath with an email to verify in the application that is locked should show email locked page" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails.afterEmailAddressProvided))
    EmailVerificationStubs.stubEmailStatusLocked(tdAll.credentials.providerId, tdAll.individualEmailAddress)
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.OK

    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "We could not confirm your identity - Apply for an agent services account - GOV.UK"
