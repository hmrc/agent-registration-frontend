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
import uk.gov.hmrc.agentregistration.shared.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.ApplicantRoleInLlp
import uk.gov.hmrc.agentregistration.shared.CompaniesHouseNameQuery
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseNameQueryForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class MemberNameControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/applicant/member-name"
  private val validApplication = tdAll.llpAgentApplication
    .modify(_.applicantContactDetails)
    .setTo(Some(ApplicantContactDetails(applicantRoleInLlp = ApplicantRoleInLlp.Member)))

  "routes should have correct paths and methods" in:
    routes.MemberNameController.show shouldBe Call(
      method = "GET",
      url = path
    )
    routes.MemberNameController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    routes.MemberNameController.submit.url shouldBe routes.MemberNameController.show.url

  s"GET $path should return 200 and render page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(validApplication)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What is your name? - Apply for an agent services account - GOV.UK"

  s"POST $path with first and last names should save data and redirect to the show name matches page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(validApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication(
      validApplication
        .modify(_.applicantContactDetails)
        .setTo(Some(ApplicantContactDetails(
          applicantRoleInLlp = ApplicantRoleInLlp.Member,
          memberNameQuery = Some(CompaniesHouseNameQuery("First", "Last"))
        )))
    )
    val response: WSResponse =
      post(path)(Map(
        CompaniesHouseNameQueryForm.firstNameKey -> Seq("First"),
        CompaniesHouseNameQueryForm.lastNameKey -> Seq("Last")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.MemberNameController.showMemberNameMatches.url

  s"POST $path with blank inputs should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(validApplication)
    val response: WSResponse =
      post(path)(Map(
        CompaniesHouseNameQueryForm.firstNameKey -> Seq(""),
        CompaniesHouseNameQueryForm.lastNameKey -> Seq("")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your name? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#firstName-error").text() shouldBe "Error: Enter your first name"
    doc.mainContent.select("#lastName-error").text() shouldBe "Error: Enter your last name"

  s"POST $path with invalid inputs should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(validApplication)
    val response: WSResponse =
      post(path)(Map(
        CompaniesHouseNameQueryForm.firstNameKey -> Seq("()))"),
        CompaniesHouseNameQueryForm.lastNameKey -> Seq(";[[[")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your name? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#firstName-error").text() shouldBe "Error: Your first name must only include letters a to z, hyphens, apostrophes and spaces"
    doc.mainContent.select("#lastName-error").text() shouldBe "Error: Your last name must only include letters a to z, hyphens, apostrophes and spaces"

  s"POST $path with save for later and valid selection should save data and redirect to the saved for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(validApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication(
      validApplication
        .modify(_.applicantContactDetails)
        .setTo(Some(ApplicantContactDetails(
          applicantRoleInLlp = ApplicantRoleInLlp.Member,
          memberNameQuery = Some(CompaniesHouseNameQuery("First", "Last"))
        )))
    )
    val response: WSResponse =
      post(path)(Map(
        CompaniesHouseNameQueryForm.firstNameKey -> Seq("First"),
        CompaniesHouseNameQueryForm.lastNameKey -> Seq("Last"),
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
        CompaniesHouseNameQueryForm.firstNameKey -> Seq(""),
        CompaniesHouseNameQueryForm.lastNameKey -> Seq(""),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applicationRoutes.SaveForLaterController.show.url
