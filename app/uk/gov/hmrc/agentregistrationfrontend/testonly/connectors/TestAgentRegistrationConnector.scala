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

package uk.gov.hmrc.agentregistrationfrontend.testonly.connectors

import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.Connector
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.TestOnlyLink
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

/** Connector to the companion backend microservice's testOnly endpoints
  */
@Singleton
class TestAgentRegistrationConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(using
  ec: ExecutionContext
)
extends Connector:

  def makeTestApplication()(using
    request: RequestHeader
  ): Future[TestOnlyLink] =
    val url: URL = url"$baseUrl/create-submitted-application"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if is2xx(status) => response.json.as[TestOnlyLink]
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure("Failed to created submitted application")

  def getRecentApplications()(using
    request: RequestHeader
  ): Future[List[AgentApplication]] =
    val url: URL = url"$baseUrl/recent-applications"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if is2xx(status) => response.json.as[List[AgentApplication]]
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure("Failed to get recent applications")

  def findApplication(agentApplicationId: AgentApplicationId)(using
    request: RequestHeader
  ): Future[Option[AgentApplication]] =
    val url: URL = url"$baseUrl/application/by-agent-application-id/${agentApplicationId.value}"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if status === Status.OK => Some(response.json.as[AgentApplication])
          case status if status === Status.NO_CONTENT => None
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure("Failed to find application")

  def findIndividual(individualProvidedDetailsId: IndividualProvidedDetailsId)(using
    request: RequestHeader
  ): Future[Option[IndividualProvidedDetails]] =
    val url: URL = url"$baseUrl/individuals/by-id/${individualProvidedDetailsId.value}"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if status === Status.OK => Some(response.json.as[IndividualProvidedDetails])
          case status if status === Status.NO_CONTENT => None
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure("Failed to find IndividualProvidedDetails")

  def findIndividuals(agentApplicationId: AgentApplicationId)(using
    request: RequestHeader
  ): Future[List[IndividualProvidedDetails]] =
    val url: URL = url"$baseUrl/individuals/by-agent-application-id/${agentApplicationId.value}"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if status === Status.OK => response.json.as[List[IndividualProvidedDetails]]
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to find individuals for application id: ${agentApplicationId.value}")

  private val baseUrl: String = appConfig.agentRegistrationBaseUrl + "/agent-registration/test-only"
