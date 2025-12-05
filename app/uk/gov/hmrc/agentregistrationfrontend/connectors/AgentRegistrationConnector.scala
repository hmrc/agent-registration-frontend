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

import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.given
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.upscan.UploadDetails
import uk.gov.hmrc.agentregistration.shared.upscan.UploadId
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.given
import uk.gov.hmrc.http.HttpReads.Implicits.given
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/** Connector to the companion backend microservice
  */
@Singleton
class AgentRegistrationConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(using
  ec: ExecutionContext
)
extends RequestAwareLogging:

  def findApplication()(using
    request: AuthorisedRequest[?]
  ): Future[Option[AgentApplication]] = {
    val url = url"$baseUrl/application"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
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
        }
      }
  }

  def upsertApplication(application: AgentApplication)(using
    request: RequestHeader
  ): Future[Unit] =
    val url = url"$baseUrl/application"
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

  def findApplication(linkId: LinkId)(using
    request: RequestHeader
  ): Future[Option[AgentApplication]] =
    val url = url"$baseUrl/application/linkId/${linkId.value}"
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
              info = s"findApplication by $linkId problem"
            )

  def findApplication(agentApplicationId: AgentApplicationId)(using
    request: RequestHeader
  ): Future[Option[AgentApplication]] =
    val url = url"$baseUrl/application/by-agent-application-id/${agentApplicationId.value}"
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
              info = s"findApplication by $agentApplicationId problem"
            )

  def getBusinessPartnerRecord(utr: Utr)(using
    request: RequestHeader
  ): Future[Option[BusinessPartnerRecordResponse]] =
    val url = url"$baseUrl/business-partner-record/utr/${utr.value}"
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
              response = response,
              info = s"getBusinessPartnerRecord problem"
            )

  def initiateUpscanUpload(uploadDetails: UploadDetails)(using
    request: RequestHeader
  ): Future[Unit] =
    val url = url"$baseUrl/application/amls/upscan-initiate"
    httpClient
      .post(url)
      .withBody(Json.toJson(uploadDetails))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.CREATED => ()
          case other =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = other,
              response = response,
              info = s"initiateUpscanUpload problem"
            )

  def getUpscanStatus(uploadId: UploadId)(using
    request: RequestHeader
  ): Future[Option[UploadStatus]] =
    val url = url"$baseUrl/application/amls/upscan-status/${uploadId.value}"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => Some(response.json.as[UploadStatus])
          case Status.NO_CONTENT => None
          case other =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = other,
              response = response,
              info = s"getUpscanStatus problem"
            )

  private val baseUrl: String = appConfig.agentRegistrationBaseUrl + "/agent-registration"
