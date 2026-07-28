/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.checkfailed

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class SoleTraderHasNoSaUtrControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/sole-trader-has-no-utr"

  "SoleTraderHasNoSaUtrControllerSpec should have the correct route" in:
    AppRoutes.apply.checkfailed.SoleTraderHasNoSaUtrController.show shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should return 200 and render the exception page only when the Sole Trader has no UTR based business details" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(tdAll.agentApplicationSoleTrader.afterStarted)

    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe
      "You cannot apply for an agent services account yet - Apply for an agent services account - GOV.UK"
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()

  s"GET $path should redirect to the task list when sole trader has a UTR and GRS data has already been received" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(tdAll.agentApplicationSoleTrader.afterGrsDataReceived)

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()

  s"GET $path should redirect to the task list when application is not for a sole trader" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(tdAll.agentApplicationLlp.afterStarted)

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
