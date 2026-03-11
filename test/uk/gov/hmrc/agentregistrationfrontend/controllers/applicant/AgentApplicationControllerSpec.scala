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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.model.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationRiskingStubs

class AgentApplicationControllerSpec
extends ControllerSpec:

  private val path: String = "/agent-registration/apply"
  private val submittedPath: String = "/agent-registration/application-submitted"
  private val viewApplicationPath: String = "/agent-registration/view-application"

  object agentApplication:
    val submitted: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterDeclarationSubmitted

  "routes should have correct paths and methods" in:
    AppRoutes.apply.AgentApplicationController.startRegistration shouldBe Call(
      method = "GET",
      url = "/agent-registration/apply"
    )
    AppRoutes.apply.AgentApplicationController.applicationSubmitted shouldBe Call(
      method = "GET",
      url = "/agent-registration/application-submitted"
    )
    AppRoutes.apply.AgentApplicationController.viewSubmittedApplication shouldBe Call(
      method = "GET",
      url = "/agent-registration/view-application"
    )

  s"GET $path should redirect to agent type page" in:
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.aboutyourbusiness.AgentTypeController.show.url

  s"GET $submittedPath should check the latest status and render the confirmation page when status is ReadyForSubmission" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.submitted)
    AgentRegistrationRiskingStubs.stubGetApplicationStatus(agentApplication.submitted._id, ApplicationForRiskingStatus.ReadyForSubmission)
    val response: WSResponse = get(submittedPath)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "You’ve applied for an agent services account - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationRiskingStubs.verifyGetApplicationStatus(agentApplication.submitted._id)

  s"GET $submittedPath should check the latest status and redirect to the progress tracker page when status is anything other than ReadyForSubmission" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.submitted)
    AgentRegistrationRiskingStubs.stubGetApplicationStatus(agentApplication.submitted._id, ApplicationForRiskingStatus.SubmittedForRisking)
    val response: WSResponse = get(submittedPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.AgentApplicationController.viewApplicationProgress.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationRiskingStubs.verifyGetApplicationStatus(agentApplication.submitted.agentApplicationId)

  s"GET $viewApplicationPath should return OK" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.submitted)
    val response: WSResponse = get(viewApplicationPath)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Application for Test Company Name - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
