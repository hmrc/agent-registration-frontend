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
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class DeclarationControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/agent-declaration/confirm-declaration"

  "route should have correct path and method" in:
    AppRoutes.apply.DeclarationController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.DeclarationController.submit shouldBe Call(
      method = "POST",
      url = path
    )
  AppRoutes.apply.DeclarationController.submit.url shouldBe AppRoutes.apply.DeclarationController.show.url

  object agentApplication:

    val beforeAllTasksComplete: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterGrsDataReceived

    val afterAllOtherTasksComplete: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterHmrcStandardForAgentsAgreed

    val afterDeclarationSubmitted: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterDeclarationSubmitted

  s"GET $path before completing all other tasks should redirect to the tasklist" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeAllTasksComplete)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path after all tasks are complete should return 200 and render page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterAllOtherTasksComplete)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Declaration - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with accept and send should update the application state and redirect to the submitted page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.afterAllOtherTasksComplete,
      updatedApplication = agentApplication.afterDeclarationSubmitted
    )
    val response: WSResponse =
      post(path)(
        Map("submit" -> Seq("AcceptAndSend"))
      )

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.AgentApplicationController.applicationSubmitted.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()
