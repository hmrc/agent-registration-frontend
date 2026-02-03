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
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.AuthorisedRequest2
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

/** Connector to the companion backend microservice
  */
@Singleton
class AgentRegistrationConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(using
  ExecutionContext
)
extends Connector:

  def findApplication2()(using
    AuthorisedRequest2[?]
  ): Future[Option[AgentApplication]] =
    val url: URL = url"$baseUrl/application"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => Some(response.json.as[AgentApplication])
          case Status.NO_CONTENT => None
          case other =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = other,
              response = response,
              info = "findApplication problem"
            )
      .andLogOnFailure(s"Failed to find Agent Application")

  def upsertApplication(application: AgentApplication)(using
    RequestHeader
  ): Future[Unit] =
    val url: URL = url"$baseUrl/application"
    httpClient
      .post(url)
      .withBody(Json.toJson(application))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => ()
          case other =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = other,
              response = response,
              info = "upsertApplication problem"
            )
      .andLogOnFailure(s"Failed to upsert Agent Application: ${application.agentApplicationId}")

  def findApplication(linkId: LinkId)(using
    RequestHeader
  ): Future[Option[AgentApplication]] =
    val url: URL = url"$baseUrl/application/linkId/${linkId.value}"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => Some(response.json.as[AgentApplication])
          case Status.NO_CONTENT => None
          case other =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = other,
              response = response
            )
      .andLogOnFailure(s"Failed to find Agent Application by link-id: $linkId")

  def findApplication(agentApplicationId: AgentApplicationId)(using
    request: RequestHeader
  ): Future[Option[AgentApplication]] =
    val url: URL = url"$baseUrl/application/by-agent-application-id/${agentApplicationId.value}"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => Some(response.json.as[AgentApplication])
          case Status.NO_CONTENT => None
          case other =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = other,
              response = response
            )
      .andLogOnFailure(s"Failed to find Agent Application by agentApplicationId: $agentApplicationId")

  def getBusinessPartnerRecord(utr: Utr)(using
    request: RequestHeader
  ): Future[Option[BusinessPartnerRecordResponse]] =
    val url: URL = url"$baseUrl/business-partner-record/utr/${utr.value}"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => Some(response.json.as[BusinessPartnerRecordResponse])
          case Status.NO_CONTENT => None
          case other =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = other,
              response = response
            )
      .andLogOnFailure(s"Failed to get business partner record")

  private val baseUrl: String = appConfig.agentRegistrationBaseUrl + "/agent-registration"
