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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

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
    routes.AgentApplicationController.startRegistration shouldBe Call(
      method = "GET",
      url = "/agent-registration/apply"
    )
    routes.AgentApplicationController.applicationSubmitted shouldBe Call(
      method = "GET",
      url = "/agent-registration/application-submitted"
    )
    routes.AgentApplicationController.viewSubmittedApplication shouldBe Call(
      method = "GET",
      url = "/agent-registration/view-application"
    )

  s"GET $path should redirect to agent type page" in:
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe aboutyourbusiness.routes.AgentTypeController.show.url

  s"GET $submittedPath should return OK" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.submitted)
    val response: WSResponse = get(submittedPath)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "You’ve finished the first stage of the application - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"GET $viewApplicationPath should return OK" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.submitted)
    val response: WSResponse = get(viewApplicationPath)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Application for Test Company Name - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
