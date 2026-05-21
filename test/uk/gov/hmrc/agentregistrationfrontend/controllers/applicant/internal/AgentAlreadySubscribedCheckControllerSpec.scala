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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.internal

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class AgentAlreadySubscribedCheckControllerSpec
extends ControllerSpec:

  object agentApplication:

    val afterUnifiedCustomerRegistryUpdateIdentifiers: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterUnifiedCustomerRegistryUpdateIdentifiers

  private val arn: String = tdAll.arn
  private val utr: Utr = agentApplication.afterUnifiedCustomerRegistryUpdateIdentifiers.getUtr

  private val path: String = "/agent-registration/apply/internal/agent-already-subscribed-check"
  private val agentAlreadySubscribedPath: String = "/agent-registration/apply/agent-already-subscribed"
  private val nextUrl: String = "/agent-registration/apply/task-list"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.internal.AgentAlreadySubscribedCheckController.check() shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should redirect to task list when business is not already registered as an agent" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterUnifiedCustomerRegistryUpdateIdentifiers)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = utr,
      responseBody = tdAll.businessPartnerRecordResponse
    )

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(utr)

  s"GET $path should redirect to wrong logged in account page when business is already registered as agent" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterUnifiedCustomerRegistryUpdateIdentifiers)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = utr,
      responseBody = tdAll.businessPartnerRecordResponse.copy(
        agentReferenceNumber = Some(arn),
        isAnASAgent = true
      )
    )

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe agentAlreadySubscribedPath
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(utr)
