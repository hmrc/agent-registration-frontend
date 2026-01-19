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

package uk.gov.hmrc.agentregistrationfrontend.connectors.llp

import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.given
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedRequest
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
class IndividualProvidedDetailsConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(using
  ec: ExecutionContext
)
extends RequestAwareLogging:

  def upsertMemberProvidedDetails(memberProvidedDetails: IndividualProvidedDetails)(using
    request: IndividualAuthorisedRequest[?]
  ): Future[Unit] = httpClient
    .post(url"$baseUrl/member-provided-details")
    .withBody(Json.toJson(memberProvidedDetails))
    .execute[HttpResponse]
    .map { response =>
      response.status match {
        case Status.OK => ()
        case other => Errors.throwServerErrorException(s"Unexpected status in the http response: $other.")
      }
    }

  def find(agentApplicationId: AgentApplicationId)(using
    request: IndividualAuthorisedRequest[?]
  ): Future[Option[IndividualProvidedDetails]] = httpClient
    .get(url"$baseUrl/member-provided-details/by-agent-applicationId/${agentApplicationId.value}")
    .execute[HttpResponse]
    .map { response =>
      response.status match {
        case Status.OK => Some(response.json.as[IndividualProvidedDetails])
        case Status.NO_CONTENT => None
        case other => Errors.throwServerErrorException(s"Unexpected status in the http response: $other.")
      }
    }

  def findAll()(using
    request: IndividualAuthorisedRequest[?]
  ): Future[List[IndividualProvidedDetails]] = httpClient
    .get(url"$baseUrl/member-provided-details")
    .execute[HttpResponse]
    .map { response =>
      response.status match {
        case Status.OK => response.json.as[List[IndividualProvidedDetails]]
        case Status.NO_CONTENT => List.empty[IndividualProvidedDetails]
        case other => Errors.throwServerErrorException(s"Unexpected status in the http response: $other.")
      }
    }

  private val baseUrl: String = appConfig.agentRegistrationBaseUrl + "/agent-registration"
