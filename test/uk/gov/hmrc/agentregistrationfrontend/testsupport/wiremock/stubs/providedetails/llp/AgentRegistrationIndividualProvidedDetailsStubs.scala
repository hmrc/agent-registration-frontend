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

import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object AgentRegistrationIndividualProvidedDetailsStubs {

  private val base = "/agent-registration/individual-provided-details"

  def stubFindIndividualProvidedDetails(
    providedDetails: IndividualProvidedDetails
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"$base/for-application/${providedDetails.agentApplicationId.value}"),
    responseStatus = Status.OK,
    responseBody = Json.toJson(List(providedDetails)).toString
  )

  def stubFindIndividualProvidedDetailsNoContent(agentApplicationId: AgentApplicationId): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"$base/for-application/${agentApplicationId.value}"),
    responseStatus = Status.OK,
    responseBody = "[]"
  )

  def stubFindAllIndividualProvidedDetails(
    providedDetailsList: List[IndividualProvidedDetails],
    agentApplicationId: AgentApplicationId
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"$base/for-matching-application/${agentApplicationId.value}"),
    responseStatus = Status.OK,
    responseBody = Json.toJson(providedDetailsList).toString
  )

  def stubFindAllIndividualProvidedDetailsNoContent(): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"$base"),
    responseStatus = Status.NO_CONTENT
  )

  def stubUpsertIndividualProvidedDetails(individualProvidedDetails: IndividualProvidedDetails): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.PUT,
    urlPattern = urlMatching(s"$base/for-individual"),
    responseStatus = Status.OK
  )

  def stubGetBusinessPartnerRecord(
    utr: Utr,
    responseBody: BusinessPartnerRecordResponse
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/agent-registration/application-business-partner-record/utr/${utr.value}"),
    responseStatus = Status.OK,
    responseBody = Json.toJson(responseBody).toString()
  )

  def verifyFind(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlPathEqualTo(base),
    count = count
  )

  def verifyFindAllForApplicationId(
    agentApplicationId: AgentApplicationId,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlPathEqualTo(s"$base/for-matching-application/${agentApplicationId.value}"),
    count = count
  )

  def verifyFindApplicationByLinkId(
    linkId: LinkId,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlPathEqualTo(s"agent-registration/application/linkId/${linkId.value}"),
    count = count
  )

  def verifyUpsert(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlPathEqualTo(base),
    count = count
  )

  def verifyUpsertIndividualProvidedDetails(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.PUT,
    urlPattern = urlPathEqualTo(s"$base/for-individual"),
    count = count
  )

}
