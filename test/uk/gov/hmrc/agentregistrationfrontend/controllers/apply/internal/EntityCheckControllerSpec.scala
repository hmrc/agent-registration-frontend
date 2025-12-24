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

class EntityCheckControllerSpec
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

    val afterHmrcEntityVerificationPass =
      tdAll
        .agentApplicationLlp
        .afterHmrcEntityVerificationPass

    val afterHmrcEntityVerificationFail =
      tdAll
        .agentApplicationLlp
        .afterHmrcEntityVerificationFail

  private val path: String = "/agent-registration/apply/internal/entity-check/verify-entity"
//  private val startGrsJourneyPath: String = "/agent-registration/apply/internal/grs/start-journey"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.internal.EntityCheckController.verifyEntity() shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should update application with pass status and redirect to task list when agent pass entity verification checks" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterGrsDataProvided)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterHmrcEntityVerificationPass)
    AgentAssuranceStubs.stubIsRefusedToDealWith(saUtr = saUtr.value, isRefused = false)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
    AgentAssuranceStubs.verifyIsRefusedToDealWith(saUtr.value)

  s"GET $path should update application with fail status and open entity checks fail page when agent fail entity verification checks" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterGrsDataProvided)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterHmrcEntityVerificationFail)
    AgentAssuranceStubs.stubIsRefusedToDealWith(saUtr = saUtr.value, isRefused = true)
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Entity verification failed... - Apply for an agent services account - GOV.UK"
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
    AgentAssuranceStubs.verifyIsRefusedToDealWith(saUtr.value)

  s"GET $path should redirect to start registration page when GRS business details not defined" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeGrsDataProvided)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.AgentApplicationController.startRegistration.url
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
