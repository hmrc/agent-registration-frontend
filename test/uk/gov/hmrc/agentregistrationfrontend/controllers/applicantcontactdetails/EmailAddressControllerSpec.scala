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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicantcontactdetails

import com.softwaremill.quicklens.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.EmailAddressForm
import uk.gov.hmrc.agentregistrationfrontend.model.Email
import uk.gov.hmrc.agentregistrationfrontend.model.VerifyEmailRequest
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.EmailVerificationStubs

class EmailAddressControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/applicant/email-address"
  private val verifyPath = "/agent-registration/apply/applicant/verify-email-address"
  private val validApplication = tdAll.llpAgentApplication
    .modify(_.applicantContactDetails)
    .setTo(Some(ApplicantContactDetails(
      applicantName = ApplicantName.NameOfAuthorised(name = Some("First Last")),
      telephoneNumber = Some(TelephoneNumber("01234567890")),
      emailAddress = None
    )))
  private val applicantEmailVerificationRequest: VerifyEmailRequest = VerifyEmailRequest(
    credId = tdAll.credentials.providerId,
    continueUrl = "http://localhost:22201/agent-registration/apply/applicant/verify-email-address",
    origin = "HMRC Agent Services",
    deskproServiceName = None,
    accessibilityStatementUrl = "/agent-services-account",
    email = Some(Email(
      address = "user@test.com",
      enterUrl = "http://localhost:22201/agent-registration/apply/applicant/email-address"
    )),
    lang = Some("en"),
    backUrl = Some("http://localhost:22201/agent-registration/apply/applicant/email-address"),
    pageTitle = None
  )

  "routes should have correct paths and methods" in:
    routes.EmailAddressController.show shouldBe Call(
      method = "GET",
      url = path
    )
    routes.EmailAddressController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    routes.EmailAddressController.submit.url shouldBe routes.EmailAddressController.show.url

  s"GET $path should return 200 and render page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(validApplication)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What is your email address? - Apply for an agent services account - GOV.UK"

  s"POST $path with well formed email address should save data and redirect to the verify endpoint" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(validApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication(
      validApplication
        .modify(_.applicantContactDetails.each.emailAddress)
        .setTo(Some(ApplicantEmailAddress(
          emailToVerify = EmailAddress(tdAll.applicantEmailAddress),
          verifiedEmail = None
        )))
    )
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq("user@test.com")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.EmailAddressController.verify.url

  s"POST $path with blank inputs should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(validApplication)
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq("")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your email address? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#emailAddress-error").text() shouldBe "Error: Enter your email address"

  s"POST $path with invalid characters should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(validApplication)
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq("[[)(*%")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your email address? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      "#emailAddress-error"
    ).text() shouldBe "Error: Enter your email address with a name, @ symbol and a domain name, like yourname@example.com"

  s"POST $path with save for later and valid email address should save data and redirect to the saved for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(validApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication(
      validApplication
        .modify(_.applicantContactDetails.each.emailAddress)
        .setTo(Some(ApplicantEmailAddress(
          emailToVerify = EmailAddress(tdAll.applicantEmailAddress),
          verifiedEmail = None
        )))
    )
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq("user@test.com"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applicationRoutes.SaveForLaterController.show.url

  s"POST $path with save for later and invalid inputs should not return errors and redirect to save for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(validApplication)
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq("[[)(*%"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applicationRoutes.SaveForLaterController.show.url

  s"GET $verifyPath with an email to verify in the application should redirect to the email verification frontend" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(
      validApplication
        .modify(_.applicantContactDetails.each.emailAddress)
        .setTo(Some(ApplicantEmailAddress(
          emailToVerify = EmailAddress(tdAll.applicantEmailAddress),
          verifiedEmail = None
        )))
    )
    EmailVerificationStubs.stubEmailStatusUnverified(tdAll.credentials.providerId)
    EmailVerificationStubs.stubVerificationRequest(applicantEmailVerificationRequest)
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "http://localhost:9890/response-url"

  s"GET $verifyPath with an already verified email not yet stored in the application should redirect to check your answers" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(
      validApplication
        .modify(_.applicantContactDetails.each.emailAddress)
        .setTo(Some(ApplicantEmailAddress(
          emailToVerify = EmailAddress(tdAll.applicantEmailAddress),
          verifiedEmail = None
        )))
    )
    EmailVerificationStubs.stubEmailStatusVerified(
      credId = tdAll.credentials.providerId,
      email = tdAll.applicantEmailAddress
    )
    AgentRegistrationStubs.stubUpdateAgentApplication(
      validApplication
        .modify(_.applicantContactDetails.each.emailAddress)
        .setTo(Some(ApplicantEmailAddress(
          emailToVerify = EmailAddress(tdAll.applicantEmailAddress),
          verifiedEmail = Some(EmailAddress(tdAll.applicantEmailAddress))
        )))
    )
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"GET $verifyPath with an already verified email stored in the application should redirect to check your answers" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(
      validApplication
        .modify(_.applicantContactDetails.each.emailAddress)
        .setTo(Some(ApplicantEmailAddress(
          emailToVerify = EmailAddress(tdAll.applicantEmailAddress),
          verifiedEmail = Some(EmailAddress(tdAll.applicantEmailAddress))
        )))
    )
    val response: WSResponse = get(verifyPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url
