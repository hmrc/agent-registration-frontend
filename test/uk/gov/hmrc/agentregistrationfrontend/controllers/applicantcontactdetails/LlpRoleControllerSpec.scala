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
import uk.gov.hmrc.agentregistration.shared.LlpRole
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.LlpRoleForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class LlpRoleControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/about-applicant/llp-member"

  "routes should have correct paths and methods" in:
    routes.LlpRoleController.show shouldBe Call(
      method = "GET",
      url = path
    )
    routes.LlpRoleController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    routes.LlpRoleController.submit.url shouldBe routes.LlpRoleController.show.url

  s"GET $path should return 200 and render page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(tdAll.llpAgentApplication)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Are you a member of the limited liability partnership? - Apply for an agent services account - GOV.UK"

  s"POST $path with Yes should redirect to the member name page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(tdAll.llpAgentApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication(
      tdAll.llpAgentApplication
        .modify(_.applicantContactDetails)
        .setTo(Some(ApplicantContactDetails(llpRole = LlpRole.Member)))
    )
    val response: WSResponse = post(path)(Map(LlpRoleForm.key -> Seq("Member")))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.MemberNameController.show.url

  s"POST $path with No should redirect to applicant name page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(tdAll.llpAgentApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication(
      tdAll.llpAgentApplication
        .modify(_.applicantContactDetails)
        .setTo(Some(ApplicantContactDetails(llpRole = LlpRole.Authorised)))
    )
    val response: WSResponse = post(path)(Map(LlpRoleForm.key -> Seq("Authorised")))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.ApplicantNameController.show.url

  s"POST $path without valid selection should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(tdAll.llpAgentApplication)
    val response: WSResponse = post(path)(Map(LlpRoleForm.key -> Seq("")))

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.title() shouldBe "Error: Are you a member of the limited liability partnership? - Apply for an agent services account - GOV.UK"

  s"POST $path with save for later and valid selection should redirect to the saved for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(tdAll.llpAgentApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication(
      tdAll.llpAgentApplication
        .modify(_.applicantContactDetails)
        .setTo(Some(ApplicantContactDetails(llpRole = LlpRole.Member)))
    )
    val response: WSResponse =
      post(path)(Map(
        LlpRoleForm.key -> Seq("Member"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applicationRoutes.SaveForLaterController.show.url

  s"POST $path with save for later and invalid selection should not return errors and redirect to save for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(tdAll.llpAgentApplication)
    val response: WSResponse =
      post(path)(Map(
        LlpRoleForm.key -> Seq(""),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applicationRoutes.SaveForLaterController.show.url
