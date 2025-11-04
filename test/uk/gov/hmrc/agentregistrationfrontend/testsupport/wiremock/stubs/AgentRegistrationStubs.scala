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

import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object AgentRegistrationStubs {

  def stubGetAgentApplication(agentApplication: AgentApplication): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching("/agent-registration/application"),
    responseStatus = 200,
    responseBody = Json.toJson(agentApplication).toString
  )

  def stubGetAgentApplicationNoContent(): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching("/agent-registration/application"),
    responseStatus = Status.NO_CONTENT,
    responseBody = ""
  )

  def verifyGetAgentApplication(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching("/agent-registration/application"),
    count = count
  )

  def stubUpdateAgentApplication(agentApplication: AgentApplication): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlMatching("/agent-registration/application"),
    responseStatus = 200,
    requestBody = Some(equalToJson(Json.toJson(agentApplication).toString))
  )

  def verifyUpdateAgentApplication(): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlMatching("/agent-registration/application")
  )

  def stubFindApplicationByLinkId(
    linkId: LinkId,
    agentApplication: AgentApplication
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/agent-registration/application/linkId/${linkId.value}"),
    responseStatus = 200,
    responseBody = Json.toJson(agentApplication).toString
  )

}
