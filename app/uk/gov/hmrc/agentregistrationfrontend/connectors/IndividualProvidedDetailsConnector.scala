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

package uk.gov.hmrc.agentregistrationfrontend.connectors

import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

/** Connector to the companion backend microservice
  */
@Singleton
class IndividualProvidedDetailsConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(using
  ExecutionContext
)
extends Connector:

  // different to upsert as used by individuals to update existing records as created by agent user when building their application,
  // whereas upsertForIndividual is used by individuals to create or update their matched provided details record during the individual journey
  def upsertForIndividual(individualProvidedDetails: IndividualProvidedDetails)(using
    request: RequestHeader
  ): Future[Unit] =
    val url: URL = url"$baseUrl/individual-provided-details/for-individual"
    httpClient
      .put(url)
      .withBody(Json.toJson(individualProvidedDetails))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure("Failed to upsert IndividualProvidedDetails for individual")

  def upsert(individualProvidedDetails: IndividualProvidedDetails)(using
    request: RequestHeader
  ): Future[Unit] =
    val url: URL = url"$baseUrl/individual-provided-details/for-application"
    httpClient
      .post(url)
      .withBody(Json.toJson(individualProvidedDetails))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure("Failed to upsert preCreated IndividualProvidedDetails")

  // for use by agent applicants when building lists of individuals
  def findAll(agentApplicationId: AgentApplicationId)(using
    request: RequestHeader
  ): Future[List[IndividualProvidedDetails]] =
    val url: URL = url"$baseUrl/individual-provided-details/for-application/${agentApplicationId.value}"
    httpClient
      .get(url)
      .execute[List[IndividualProvidedDetails]]
      .andLogOnFailure(s"Failed to find IndividualProvidedDetails by agent application id: ${agentApplicationId.value}")

  def findAllForMatching(agentApplicationId: AgentApplicationId)(using
    request: RequestHeader
  ): Future[List[IndividualProvidedDetails]] =
    val url: URL = url"$baseUrl/individual-provided-details/for-matching-application/${agentApplicationId.value}"
    httpClient
      .get(url)
      .execute[List[IndividualProvidedDetails]]
      .andLogOnFailure(s"Failed to find IndividualProvidedDetails for matching by agent application id: ${agentApplicationId.value}")

  def findById(individualProvidedDetailsId: IndividualProvidedDetailsId)(using
    RequestHeader
  ): Future[Option[IndividualProvidedDetails]] =
    val url: URL = url"$baseUrl/individual-provided-details/by-id/${individualProvidedDetailsId.value}"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => Some(response.json.as[IndividualProvidedDetails])
          case Status.NO_CONTENT => None
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to find IndividualProvidedDetails by id: ${individualProvidedDetailsId.value}")

  def delete(individualProvidedDetailsId: IndividualProvidedDetailsId)(using
    request: RequestHeader
  ): Future[Unit] =
    val url: URL = url"$baseUrl/individual-provided-details/delete-by-id/${individualProvidedDetailsId.value}"
    httpClient
      .delete(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "DELETE",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to delete IndividualProvidedDetails by id: ${individualProvidedDetailsId.value}")

  private val baseUrl: String = appConfig.agentRegistrationBaseUrl + "/agent-registration"
