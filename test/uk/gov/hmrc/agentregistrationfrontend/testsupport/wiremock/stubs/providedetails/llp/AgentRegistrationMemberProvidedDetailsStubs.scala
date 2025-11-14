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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp

import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object AgentRegistrationMemberProvidedDetailsStubs {

  private val base = "/agent-registration/member-provided-details"

  def stubFindMemberProvidedDetails(
    providedDetails: MemberProvidedDetails
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"$base/by-agent-applicationId/${providedDetails.agentApplicationId.value}"),
    responseStatus = Status.OK,
    responseBody = Json.toJson(providedDetails).toString
  )

  def stubFindMemberProvidedDetailsNoContent(agentApplicationId: AgentApplicationId): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"$base/by-agent-applicationId/${agentApplicationId.value}"),
    responseStatus = Status.NO_CONTENT
  )

  def stubFindAllMemberProvidedDetails(
    providedDetailsList: List[MemberProvidedDetails]
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"$base"),
    responseStatus = Status.OK,
    responseBody = Json.toJson(providedDetailsList).toString
  )

  def stubFindAllMemberProvidedDetailsNoContent(): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"$base"),
    responseStatus = Status.NO_CONTENT
  )

  def stubUpsertMemberProvidedDetails(memberProvidedDetails: MemberProvidedDetails): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlMatching(base),
    responseStatus = Status.OK,
    requestBody = Some(equalToJson(Json.toJson(memberProvidedDetails).toString))
  )

  def verifyFind(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlPathEqualTo(base),
    count = count
  )

  def verifyFindByAgentApplicationID(
    agentApplicationId: AgentApplicationId,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlPathEqualTo(s"$base/by-agent-applicationId/${agentApplicationId.value}"),
    count = count
  )

  def verifyUpsert(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlPathEqualTo(base),
    count = count
  )

}
