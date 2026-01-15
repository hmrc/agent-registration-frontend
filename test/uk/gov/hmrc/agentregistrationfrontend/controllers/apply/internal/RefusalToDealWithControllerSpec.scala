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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.internal

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData.saUtr
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentAssuranceStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class RefusalToDealWithControllerSpec
extends ControllerSpec:

  object agentApplication:

    val beforeGrsDataProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterStarted

    val afterGrsDataProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterGrsDataReceived

    val afterRefusalToDealWithCheckPass =
      tdAll
        .agentApplicationLlp
        .afterRefusalToDealWithCheckPass

    val afterRefusalToDealWithCheckFail =
      tdAll
        .agentApplicationLlp
        .afterRefusalToDealWithCheckFail

  private val path: String = "/agent-registration/apply/internal/refusal-to-deal-with-check"
  private val nextPageUrl: String = "/agent-registration/apply/internal/deceased-check"
  private val previousPage: String = "/agent-registration/apply"
  private val cannotRegisterPage: String = "/agent-registration/apply/cannot-register"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.internal.RefusalToDealWithController.check() shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should update application with pass status and redirect to company status check  when agent pass entity verification checks" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterGrsDataProvided)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterRefusalToDealWithCheckPass)
    AgentAssuranceStubs.stubIsRefusedToDealWith(saUtr = saUtr, isRefused = false)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
    AgentAssuranceStubs.verifyIsRefusedToDealWith(saUtr)

  s"GET $path should update application with fail status and open entity checks fail page when agent fail entity verification checks" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterGrsDataProvided)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterRefusalToDealWithCheckFail)
    AgentAssuranceStubs.stubIsRefusedToDealWith(saUtr = saUtr, isRefused = true)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe cannotRegisterPage
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
    AgentAssuranceStubs.verifyIsRefusedToDealWith(saUtr)

  s"GET $path should redirect to start registration page when GRS business details not defined" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeGrsDataProvided)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe previousPage
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication(0)
    AgentAssuranceStubs.verifyIsRefusedToDealWith(saUtr, 0)

  s"GET $path should redirect to deceased check when entity verification already done" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterRefusalToDealWithCheckPass)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication(0)
    AgentAssuranceStubs.verifyIsRefusedToDealWith(saUtr, 0)

  s"GET $path should run refusal to deal with check when refusal to deal with check Fail" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterRefusalToDealWithCheckFail)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterRefusalToDealWithCheckPass)
    AgentAssuranceStubs.stubIsRefusedToDealWith(saUtr = saUtr, isRefused = false)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
    AgentAssuranceStubs.verifyIsRefusedToDealWith(saUtr)
