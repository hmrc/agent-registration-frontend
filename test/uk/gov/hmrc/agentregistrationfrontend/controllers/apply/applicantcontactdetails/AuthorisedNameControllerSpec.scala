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
import uk.gov.hmrc.agentregistrationfrontend.forms.ApplicantNameForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class AuthorisedNameControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/applicant/applicant-name"

  object agentApplication:

    private val whenApplicantIsAuthorised =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAuthorised

    val beforeNameDeclared: AgentApplicationLlp =
      whenApplicantIsAuthorised
        .afterRoleSelected

    val afterNameDeclared: AgentApplicationLlp =
      whenApplicantIsAuthorised
        .afterNameDeclared

  object ExpectedStrings:

    private val heading = "What is your full name?"
    val title = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $heading - Apply for an agent services account - GOV.UK"
    val requiredError = "Enter your full name"
    val invalidError = "Your full name must only include letters a to z, hyphens, apostrophes and spaces"
    val maxLengthError = "Your full name must be 100 characters or fewer"

  "routes should have correct paths and methods" in:
    routes.AuthorisedNameController.show shouldBe Call(
      method = "GET",
      url = path
    )
    routes.AuthorisedNameController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    routes.AuthorisedNameController.submit.url shouldBe routes.AuthorisedNameController.show.url

  s"GET $path should return 200 and render page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeNameDeclared)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe ExpectedStrings.title
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with valid name should save data and redirect to check your answers" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeNameDeclared,
      updatedApplication = agentApplication.afterNameDeclared
    )
    val response: WSResponse =
      post(path)(Map(
        ApplicantNameForm.key -> Seq("Miss Alexa Fantastic")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeNameDeclared)
    val response: WSResponse =
      post(path)(Map(
        ApplicantNameForm.key -> Seq(Constants.EMPTY_STRING)
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(s"#${ApplicantNameForm.key}-error").text() shouldBe s"Error: ${ExpectedStrings.requiredError}"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with invalid characters should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeNameDeclared)
    val response: WSResponse =
      post(path)(Map(
        ApplicantNameForm.key -> Seq("[[)(*%")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(s"#${ApplicantNameForm.key}-error").text() shouldBe s"Error: ${ExpectedStrings.invalidError}"

  s"POST $path with more than 100 characters should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeNameDeclared)
    val response: WSResponse =
      post(path)(Map(
        ApplicantNameForm.key -> Seq("A".repeat(101))
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select("#authorisedName-error").text() shouldBe s"Error: ${ExpectedStrings.maxLengthError}"

  s"POST $path with save for later and valid selection should save data and redirect to the saved for later page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeNameDeclared,
      updatedApplication = agentApplication.afterNameDeclared
    )
    val response: WSResponse =
      post(path)(Map(
        ApplicantNameForm.key -> Seq("Miss Alexa Fantastic"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with save for later and invalid inputs should not return errors and redirect to save for later page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeNameDeclared)
    val response: WSResponse =
      post(path)(Map(
        ApplicantNameForm.key -> Seq("[[)(*%"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
