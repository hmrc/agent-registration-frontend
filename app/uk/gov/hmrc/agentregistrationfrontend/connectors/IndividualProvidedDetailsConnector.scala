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
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedRequest
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

  def upsertMemberProvidedDetails(individualProvidedDetails: IndividualProvidedDetailsToBeDeleted)(using
    request: IndividualAuthorisedRequest[?]
  ): Future[Unit] =
    val url: URL = url"$baseUrl/member-provided-details"
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
      .andLogOnFailure("Failed to upsert IndividualProvidedDetails")

  def find(agentApplicationId: AgentApplicationId)(using
    IndividualAuthorisedRequest[?]
  ): Future[Option[IndividualProvidedDetailsToBeDeleted]] =
    val url: URL = url"$baseUrl/member-provided-details/by-agent-applicationId/${agentApplicationId.value}"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => Some(response.json.as[IndividualProvidedDetailsToBeDeleted])
          case Status.NO_CONTENT => None
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to find IndividualProvidedDetails by agent application id: $agentApplicationId")

  // for use by agent applicants when building lists of individuals
  def find(using
    request: AgentApplicationRequest[?]
  ): Future[List[IndividualProvidedDetailsToBeDeleted]] =
    val url: URL = url"$baseUrl/individual-provided-details/for-application/${request.agentApplication.agentApplicationId.value}"
    httpClient
      .get(url)
      .execute[List[IndividualProvidedDetailsToBeDeleted]]
      .andLogOnFailure(s"Failed to find IndividualProvidedDetails by agent application id: ${request.agentApplication.agentApplicationId.value}")

  def findAll()(using
    IndividualAuthorisedRequest[?]
  ): Future[List[IndividualProvidedDetailsToBeDeleted]] =
    val url: URL = url"$baseUrl/member-provided-details"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => response.json.as[List[IndividualProvidedDetailsToBeDeleted]]
          case Status.NO_CONTENT => List.empty[IndividualProvidedDetailsToBeDeleted]
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to find IndividualProvidedDetails")

  private val baseUrl: String = appConfig.agentRegistrationBaseUrl + "/agent-registration"
