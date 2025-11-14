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
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistration.shared._
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
  ): Future[Option[AgentApplication]] = httpClient
    .get(url"$baseUrl/application")
    .execute[HttpResponse]
    .map { response =>
      response.status match {
        case Status.OK => Some(response.json.as[AgentApplication])
        case Status.NO_CONTENT => None
        case other => Errors.throwServerErrorException(s"Unexpected status in the http response: $other.")
      }
    }

  def upsertApplication(application: AgentApplication)(using
    request: RequestHeader
  ): Future[Unit] = httpClient
    .post(url"$baseUrl/application")
    .withBody(Json.toJson(application))
    .execute[HttpResponse]
    .map { response =>
      response.status match {
        case Status.OK => ()
        case other => Errors.throwServerErrorException(s"Unexpected status in the http response: $other.")
      }
    }

  def findApplication(linkId: LinkId)(using
    request: RequestHeader
  ): Future[Option[AgentApplication]] = httpClient
    .get(url"$baseUrl/application/linkId/${linkId.value}")
    .execute[HttpResponse]
    .map { response =>
      response.status match {
        case Status.OK => Some(response.json.as[AgentApplication])
        case Status.NO_CONTENT => None
        case other => Errors.throwServerErrorException(s"Unexpected status in the http response: $other.")
      }
    }

  def findApplication(agentApplicationId: AgentApplicationId)(using
    request: RequestHeader
  ): Future[Option[AgentApplication]] = httpClient
    .get(url"$baseUrl/application/by-agent-application-id/${agentApplicationId.value}")
    .execute[HttpResponse]
    .map { response =>
      response.status match {
        case Status.OK => Some(response.json.as[AgentApplication])
        case Status.NO_CONTENT => None
        case other =>
          Errors.throwServerErrorException(s"Unexpected status when searching by AgentApplicationId $agentApplicationId in the http response: $other.")
      }
    }

  def getBusinessPartnerRecord(utr: Utr)(using
    request: RequestHeader
  ): Future[Option[BusinessPartnerRecordResponse]] = httpClient
    .get(url"$baseUrl/business-partner-record/utr/${utr.value}")
    .execute[HttpResponse]
    .map { response =>
      response.status match {
        case Status.OK => Some(response.json.as[BusinessPartnerRecordResponse])
        case Status.NO_CONTENT => None
        case other => Errors.throwServerErrorException(s"Unexpected status in the http response: $other.")
      }
    }

  private val baseUrl: String = appConfig.agentRegistrationBaseUrl + "/agent-registration"
