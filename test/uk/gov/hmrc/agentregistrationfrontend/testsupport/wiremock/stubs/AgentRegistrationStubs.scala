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
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object AgentRegistrationStubs:

  def stubGetAgentApplication(agentApplication: AgentApplication): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo("/agent-registration/application"),
    responseStatus = 200,
    responseBody = Json.toJson(agentApplication).toString
  )

  def stubGetAgentApplicationNoContent(): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo("/agent-registration/application"),
    responseStatus = Status.NO_CONTENT,
    responseBody = ""
  )

  def verifyGetAgentApplication(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo("/agent-registration/application"),
    count = count
  )

  def stubUpdateAgentApplication(agentApplication: AgentApplication): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlPathEqualTo("/agent-registration/application"),
    responseStatus = 200,
    requestBody = Some(wm.equalToJson(Json.toJson(agentApplication).toString))
  )

  def stubDeleteIndividualProvidedDetails(individualProvidedDetailsId: IndividualProvidedDetailsId): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.DELETE,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/individual-provided-details/delete-by-id/${individualProvidedDetailsId.value}"),
    responseStatus = Status.OK,
    responseBody = ""
  )

  def verifyUpdateAgentApplication(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlPathEqualTo("/agent-registration/application"),
    count = count
  )

  def stubFindApplicationByLinkId(
    linkId: LinkId,
    agentApplication: AgentApplication
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/application/linkId/${linkId.value}"),
    responseStatus = 200,
    responseBody = Json.toJson(agentApplication).toString
  )

  def stubFindApplicationByLinkIdNoContent(
    linkId: LinkId
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/application/linkId/${linkId.value}"),
    responseStatus = Status.NO_CONTENT,
    responseBody = ""
  )

  def stubFindApplication(
    agentApplicationId: AgentApplicationId,
    agentApplication: AgentApplication
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/application/by-agent-application-id/${agentApplicationId.value}"),
    responseStatus = 200,
    responseBody = Json.toJson(agentApplication).toString
  )

  def stubGetBusinessPartnerRecord(
    utr: Utr,
    responseBody: BusinessPartnerRecordResponse
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlMatching(s"/agent-registration/business-partner-record/utr/${utr.value}"),
    responseStatus = Status.OK,
    responseBody = Json.toJson(responseBody).toString()
  )

  def stubGetApplicationBusinessPartnerRecord(
    utr: Utr,
    responseBody: BusinessPartnerRecordResponse
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlMatching(s"/agent-registration/application-business-partner-record/utr/${utr.value}"),
    responseStatus = Status.OK,
    responseBody = Json.toJson(responseBody).toString()
  )

  def stubFindApplicationByAgentApplicationId(
    agentApplicationId: AgentApplicationId,
    agentApplication: AgentApplication
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/application/by-agent-application-id/${agentApplicationId.value}"),
    responseStatus = 200,
    responseBody = Json.toJson(agentApplication).toString
  )

  def stubFindApplicationByAgentApplicationIdNoContent(
    agentApplicationId: AgentApplicationId
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/application/by-agent-application-id/${agentApplicationId.value}"),
    responseStatus = Status.NO_CONTENT,
    responseBody = ""
  )

  def stubFindIndividualForApplication(
    individual: IndividualProvidedDetails
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/individual-provided-details/by-id/${individual.individualProvidedDetailsId.value}"),
    responseStatus = 200,
    responseBody = Json.toJson(individual).toString
  )

  def stubFindIndividualsForApplication(
    agentApplicationId: AgentApplicationId,
    individuals: List[IndividualProvidedDetails] = List.empty
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/individual-provided-details/for-application/${agentApplicationId.value}"),
    responseStatus = 200,
    responseBody = Json.toJson(individuals).toString
  )

  def stubUpsertIndividualProvidedDetails(individualProvidedDetails: IndividualProvidedDetails): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/individual-provided-details/for-application"),
    responseStatus = Status.OK,
    requestBody = Some(equalToJson(Json.toJson(individualProvidedDetails).toString))
  )

  def verifyFindApplicationByAgentApplicationId(
    agentApplicationId: AgentApplicationId,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/application/by-agent-application-id/${agentApplicationId.value}"),
    count = count
  )

  def verifyFindApplicationByLinkId(
    linkId: LinkId,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/application/linkId/${linkId.value}"),
    count = count
  )

  def verifyGetBusinessPartnerRecord(
    utr: Utr,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/business-partner-record/utr/${utr.value}"),
    count = count
  )

  def verifyFindIndividualsForApplication(
    applicationId: AgentApplicationId,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/individual-provided-details/for-application/${applicationId.value}"),
    count = count
  )

  def verifyUpsertIndividualProvidedDetails(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/individual-provided-details/for-application"),
    count = count
  )

  def verifyDeleteIndividualProvidedDetails(
    individualProvidedDetailsId: IndividualProvidedDetailsId,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.DELETE,
    urlPattern = wm.urlPathEqualTo(s"/agent-registration/individual-provided-details/delete-by-id/${individualProvidedDetailsId.value}"),
    count = count
  )
