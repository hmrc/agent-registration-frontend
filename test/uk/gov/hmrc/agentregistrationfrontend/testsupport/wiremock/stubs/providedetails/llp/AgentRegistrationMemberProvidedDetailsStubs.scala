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
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object AgentRegistrationMemberProvidedDetailsStubs {

  def stubGetMemberProvidedDetails(memberProvidedDetails: MemberProvidedDetails): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching("/agent-registration/member-provided-details"),
    responseStatus = 200,
    responseBody = Json.toJson(memberProvidedDetails).toString
  )

  def stubGetMemberProvidedDetailsNoContent(): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching("/agent-registration/member-provided-details"),
    responseStatus = Status.NO_CONTENT
  )

  // when you want to verify what is being stored
  def stubUpsertMemberProvidedDetails(memberProvidedDetails: MemberProvidedDetails): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlMatching("/agent-registration/member-provided-details"),
    responseStatus = 200,
    requestBody = Some(equalToJson(Json.toJson(memberProvidedDetails).toString))
  )

  def stubFindMemberProvidedDetailsByApplicationId(
    agentApplicationId: AgentApplicationId,
    providedDetailsList: List[MemberProvidedDetails]
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/agent-registration/member-provided-details/application-id/${agentApplicationId.value}"),
    responseStatus = 200,
    responseBody = Json.toJson(providedDetailsList).toString
  )

}
