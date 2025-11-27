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
import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseNameQueryForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class MemberNameControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/applicant/member-name"
  private object agentApplication:

    val whenApplicantIsAMember =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAMember

    val beforeNameQueryProvided: AgentApplicationLlp =
      whenApplicantIsAMember
        .afterRoleSelected

    val afterNameQueryProvided: AgentApplicationLlp =
      whenApplicantIsAMember
        .afterNameQueryProvided

  private object ExpectedStrings:

    val heading = "What is your name?"
    val title = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $heading - Apply for an agent services account - GOV.UK"
    val firstNameRequiredError = "Enter your first name"
    val firstNameInvalidError = "Your first name must only include letters a to z, hyphens, apostrophes and spaces"
    val lastNameRequiredError = "Enter your last name"
    val lastNameInvalidError = "Your last name must only include letters a to z, hyphens, apostrophes and spaces"

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
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeNameQueryProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe ExpectedStrings.title
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with first and last names should save data and redirect to the show name matches page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeNameQueryProvided,
      updatedApplication = agentApplication.afterNameQueryProvided
    )
    val response: WSResponse =
      post(path)(Map(
        CompaniesHouseNameQueryForm.firstNameKey -> Seq(agentApplication.whenApplicantIsAMember.firstNameQuery),
        CompaniesHouseNameQueryForm.lastNameKey -> Seq(agentApplication.whenApplicantIsAMember.lastNameQuery)
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe routes.CompaniesHouseMatchingController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeNameQueryProvided)
    val response: WSResponse =
      post(path)(Map(
        CompaniesHouseNameQueryForm.firstNameKey -> Seq(""),
        CompaniesHouseNameQueryForm.lastNameKey -> Seq("")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(s"#${CompaniesHouseNameQueryForm.firstNameKey}-error").text() shouldBe s"Error: ${ExpectedStrings.firstNameRequiredError}"
    doc.mainContent.select(s"#${CompaniesHouseNameQueryForm.lastNameKey}-error").text() shouldBe s"Error: ${ExpectedStrings.lastNameRequiredError}"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with invalid inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeNameQueryProvided)
    val response: WSResponse =
      post(path)(Map(
        CompaniesHouseNameQueryForm.firstNameKey -> Seq("()))"),
        CompaniesHouseNameQueryForm.lastNameKey -> Seq(";[[[")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(s"#${CompaniesHouseNameQueryForm.firstNameKey}-error").text() shouldBe s"Error: ${ExpectedStrings.firstNameInvalidError}"
    doc.mainContent.select(s"#${CompaniesHouseNameQueryForm.lastNameKey}-error").text() shouldBe s"Error: ${ExpectedStrings.lastNameInvalidError}"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with save for later and valid selection should save data and redirect to the saved for later page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeNameQueryProvided,
      updatedApplication = agentApplication.afterNameQueryProvided
    )
    val response: WSResponse =
      post(path)(Map(
        CompaniesHouseNameQueryForm.firstNameKey -> Seq(agentApplication.whenApplicantIsAMember.firstNameQuery),
        CompaniesHouseNameQueryForm.lastNameKey -> Seq(agentApplication.whenApplicantIsAMember.lastNameQuery),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with save for later and invalid inputs should not return errors and redirect to save for later page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeNameQueryProvided)
    val response: WSResponse =
      post(path)(Map(
        CompaniesHouseNameQueryForm.firstNameKey -> Seq(""),
        CompaniesHouseNameQueryForm.lastNameKey -> Seq(""),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
