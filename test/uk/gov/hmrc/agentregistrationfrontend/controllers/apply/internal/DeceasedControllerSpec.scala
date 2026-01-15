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
import play.api.mvc.Call

import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData.nino
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.CitizenDetailsStub

class DeceasedControllerSpec
extends ControllerSpec:

  object agentApplication:

    val afterRefusalToDealWithCheckPass =
      tdAll
        .agentApplicationSoleTrader
        .afterRefusalToDealWithCheckPass

    val afterRefusalToDealWithCheckFail =
      tdAll
        .agentApplicationSoleTrader
        .afterRefusalToDealWithCheckFail

    val aterRefusalToDealWithCheckPassLlp =
      tdAll
        .agentApplicationLlp
        .afterRefusalToDealWithCheckPass

    val afterDeceasedCheckPass =
      tdAll
        .agentApplicationSoleTrader
        .afterDeceasedCheckPass

    val afterDeceasedCheckFail =
      tdAll
        .agentApplicationSoleTrader
        .afterDeceasedCheckFail

  private val path: String = "/agent-registration/apply/internal/deceased-check"
  private val nextPageUrl: String = "/agent-registration/apply/internal/companies-house-status-check"
  private val cannotRegisterPage: String = "/agent-registration/apply/cannot-confirm-identity"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.internal.DeceasedController.check() shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path when is not deceased should update application with pass status and redirect to company status check" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterRefusalToDealWithCheckPass)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterDeceasedCheckPass)
    CitizenDetailsStub.stubDesignatoryDetailsFound(nino = nino, deceased = false)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
    CitizenDetailsStub.verifyDesignatoryDetails(nino = nino)

  s"GET $path when deceased should update application with fail status and open entity checks fail page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterRefusalToDealWithCheckPass)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterDeceasedCheckFail)
    CitizenDetailsStub.stubDesignatoryDetailsFound(nino = nino, deceased = true)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe cannotRegisterPage
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
    CitizenDetailsStub.verifyDesignatoryDetails(nino = nino)

  s"GET $path should redirect to company status check when deceased check already pass" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterDeceasedCheckPass)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication(0)
    CitizenDetailsStub.verifyDesignatoryDetails(nino = nino, 0)

  s"GET $path should run deceased check when deceased check already fail" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterDeceasedCheckFail)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterDeceasedCheckPass)
    CitizenDetailsStub.stubDesignatoryDetailsFound(nino = nino, deceased = false)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
    CitizenDetailsStub.verifyDesignatoryDetails(nino = nino)

  s"GET $path should redirect to company status check when business type is not SoleTrader like LLP" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.aterRefusalToDealWithCheckPassLlp)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication(0)
    CitizenDetailsStub.verifyDesignatoryDetails(nino = nino, 0)
