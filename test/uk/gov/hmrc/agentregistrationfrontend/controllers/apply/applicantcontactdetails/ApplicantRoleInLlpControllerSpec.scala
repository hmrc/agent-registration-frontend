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
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.ApplicantRoleInLlpForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class ApplicantRoleInLlpControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/applicant/llp-member"

  private object agentApplication:

    val afterGrsDataReceived =
      tdAll
        .agentApplicationLlp
        .afterGrsDataReceived

    val whenApplicantIsAMember =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAMember

    val whenApplicantIsAuthorised =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAuthorised

  private object ExpectedStrings:

    val heading = "Are you a member of the limited liability partnership?"
    val title = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $heading - Apply for an agent services account - GOV.UK"
    val requiredError = "Select whether you are a member of the limited liability partnership"

  "routes should have correct paths and methods" in:
    routes.ApplicantRoleInLlpController.show shouldBe Call(
      method = "GET",
      url = path
    )
    routes.ApplicantRoleInLlpController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    routes.ApplicantRoleInLlpController.submit.url shouldBe routes.ApplicantRoleInLlpController.show.url

  s"GET $path should return 200 and render page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterGrsDataReceived)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe ExpectedStrings.title
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with Yes should redirect to the member name page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.afterGrsDataReceived,
      updatedApplication = agentApplication.whenApplicantIsAMember.afterRoleSelected
    )
    val response: WSResponse = post(path)(Map(ApplicantRoleInLlpForm.key -> Seq("Member")))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe routes.MemberNameController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with No should redirect to applicant name page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.afterGrsDataReceived,
      updatedApplication = agentApplication.whenApplicantIsAuthorised.afterRoleSelected
    )
    val response: WSResponse = post(path)(Map(ApplicantRoleInLlpForm.key -> Seq("Authorised")))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe routes.AuthorisedNameController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path without valid selection should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterGrsDataReceived)
    val response: WSResponse = post(path)(Map(ApplicantRoleInLlpForm.key -> Seq("")))

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.title() shouldBe ExpectedStrings.errorTitle
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with save for later and valid selection should redirect to the saved for later page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.afterGrsDataReceived,
      updatedApplication = agentApplication.whenApplicantIsAMember.afterRoleSelected
    )
    val response: WSResponse =
      post(path)(Map(
        ApplicantRoleInLlpForm.key -> Seq("Member"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with save for later and invalid selection should not return errors and redirect to save for later page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterGrsDataReceived)
    val response: WSResponse =
      post(path)(Map(
        ApplicantRoleInLlpForm.key -> Seq(""),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
