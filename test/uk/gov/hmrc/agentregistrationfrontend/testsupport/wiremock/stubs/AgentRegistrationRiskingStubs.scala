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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.risking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object AgentRegistrationRiskingStubs:

  def stubSubmitAgentApplication(submitForRiskingRequest: SubmitForRiskingRequest): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration-risking/submit-for-risking"),
    responseStatus = 201
  )

  def verifySubmitAgentApplication(
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration-risking/submit-for-risking"),
    count = count
  )

  def stubGetApplicationStatus(
    agentApplicationId: AgentApplicationId,
    status: ApplicationForRiskingStatus
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration-risking/application-status/${agentApplicationId.value}"),
    responseStatus = 200,
    responseBody = s"""{"status": "${status.toString}"}"""
  )

  def verifyGetApplicationStatus(
    agentApplicationId: AgentApplicationId,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration-risking/application-status/${agentApplicationId.value}"),
    count = count
  )
