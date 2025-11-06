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

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/applicant/check-your-answers"

  "route should have correct path and method" in:
    routes.CheckYourAnswersController.show shouldBe Call(
      method = "GET",
      url = path
    )

  object agentApplication:

    val complete: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAuthorised
        .afterEmailAddressVerified

    val missingVerifiedEmail: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAuthorised
        .afterEmailAddressProvided

    val missingEmail: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAuthorised
        .afterTelephoneNumberProvided

    val missingTelephone: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAuthorised
        .afterNameDeclared

    val missingCompaniesHouseOfficerSelection: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAMember
        .afterNameQueryProvided

    val missingMemberNameQuery: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAMember
        .afterRoleSelected

    val missingAuthorisedName: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAuthorised
        .afterRoleSelected

    val noContactDetails: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterGrsDataReceived

  private case class TestCaseForCya(
    application: AgentApplicationLlp,
    name: String,
    expectedRedirect: Option[String] = None
  )

  List(
    TestCaseForCya(
      application = agentApplication.complete,
      name = "complete contact details"
    ),
    TestCaseForCya(
      application = agentApplication.missingVerifiedEmail,
      name = "missing verified email address",
      expectedRedirect = Some(routes.EmailAddressController.show.url)
    ),
    TestCaseForCya(
      application = agentApplication.missingEmail,
      name = "missing email address",
      expectedRedirect = Some(routes.EmailAddressController.show.url)
    ),
    TestCaseForCya(
      application = agentApplication.missingTelephone,
      name = "missing telephone number",
      expectedRedirect = Some(routes.TelephoneNumberController.show.url)
    ),
    TestCaseForCya(
      application = agentApplication.missingCompaniesHouseOfficerSelection,
      name = "missing companies house office selection",
      expectedRedirect = Some(routes.CompaniesHouseMatchingController.show.url)
    ),
    TestCaseForCya(
      application = agentApplication.missingMemberNameQuery,
      name = "missing name to query companies house with",
      expectedRedirect = Some(routes.MemberNameController.show.url)
    ),
    TestCaseForCya(
      application = agentApplication.missingAuthorisedName,
      name = "missing authorised applicant name",
      expectedRedirect = Some(routes.AuthorisedNameController.show.url)
    ),
    TestCaseForCya(
      application = agentApplication.noContactDetails,
      name = "missing all contact details",
      expectedRedirect = Some(routes.ApplicantRoleInLlpController.show.url)
    )
  ).foreach: testCase =>
    if testCase.expectedRedirect.isEmpty then
      s"GET $path with complete contact details should return 200 and render page" in:
        AuthStubs.stubAuthorise()
        AgentRegistrationStubs.stubGetAgentApplication(testCase.application)
        val response: WSResponse = get(path)

        response.status shouldBe 200
        val doc = response.parseBodyAsJsoupDocument
        doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"
        doc.select("h2.govuk-caption-l").text() shouldBe "Applicant contact details"
    else
      s"GET $path with ${testCase.name} should redirect to the email entry page" in:
        AuthStubs.stubAuthorise()
        AgentRegistrationStubs.stubGetAgentApplication(testCase.application)
        val response: WSResponse = get(path)

        response.status shouldBe 303
        response.header("Location").value shouldBe testCase.expectedRedirect.get
