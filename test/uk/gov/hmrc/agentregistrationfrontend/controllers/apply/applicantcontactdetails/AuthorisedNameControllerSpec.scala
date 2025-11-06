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

import com.softwaremill.quicklens.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.routes as applyRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.AuthorisedNameForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class AuthorisedNameControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/applicant/applicant-name"
  private val validApplication =
    tdAll
      .agentApplicationLlp
      .sectionContactDetails
      .whenApplicantIsAuthorised
      .afterRoleSelected

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
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(validApplication)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What is your full name? - Apply for an agent services account - GOV.UK"

  s"POST $path with valid name should save data and redirect to check your answers" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(validApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication(
      validApplication
        .modify(_.applicantContactDetails)
        .setTo(Some(ApplicantContactDetails(
          applicantName = ApplicantName.NameOfAuthorised(
            name = Some("First Last")
          )
        )))
    )
    val response: WSResponse =
      post(path)(Map(
        AuthorisedNameForm.key -> Seq("First Last")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"POST $path with blank inputs should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(validApplication)
    val response: WSResponse =
      post(path)(Map(
        AuthorisedNameForm.key -> Seq("")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your full name? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#authorisedName-error").text() shouldBe "Error: Enter your full name"

  s"POST $path with invalid characters should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(validApplication)
    val response: WSResponse =
      post(path)(Map(
        AuthorisedNameForm.key -> Seq("[[)(*%")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your full name? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#authorisedName-error").text() shouldBe "Error: Your full name must only include letters a to z, hyphens, apostrophes and spaces"

  s"POST $path with more than 100 characters should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(validApplication)
    val response: WSResponse =
      post(path)(Map(
        AuthorisedNameForm.key -> Seq("A".repeat(101))
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your full name? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#authorisedName-error").text() shouldBe "Error: Your full name must be 100 characters or fewer"

  s"POST $path with save for later and valid selection should save data and redirect to the saved for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(validApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication(
      validApplication
        .modify(_.applicantContactDetails)
        .setTo(Some(ApplicantContactDetails(
          applicantName = ApplicantName.NameOfAuthorised(
            name = Some("First Last")
          )
        )))
    )
    val response: WSResponse =
      post(path)(Map(
        AuthorisedNameForm.key -> Seq("First Last"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applyRoutes.SaveForLaterController.show.url

  s"POST $path with save for later and invalid inputs should not return errors and redirect to save for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(validApplication)
    val response: WSResponse =
      post(path)(Map(
        AuthorisedNameForm.key -> Seq("[[)(*%"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applyRoutes.SaveForLaterController.show.url
