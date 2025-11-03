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

package uk.gov.hmrc.agentregistrationfrontend.connectors.providedetails

import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.given
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.*
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
class ProvideDetailsConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(using
  ec: ExecutionContext
)
extends RequestAwareLogging:

  def findProvidedDetails(linkId: LinkId)(using
    request: IndividualAuthorisedRequest[?]
  ): Future[Option[ProvidedDetails]] = httpClient
    .get(url"$baseUrl/provideddetails/linkId/$linkId")
    .execute[HttpResponse]
    .map { response =>
      response.status match {
        case Status.OK => Some(response.json.as[ProvidedDetails])
        case Status.NO_CONTENT => None
        case other => Errors.throwServerErrorException(s"Unexpected status in the http response: $other.")
      }
    }

  def upsertProvidedDetails(providedDetails: ProvidedDetails)(using
    request: RequestHeader
  ): Future[Unit] = httpClient
    .post(url"$baseUrl/provideddetails")
    .withBody(Json.toJson(providedDetails))
    .execute[HttpResponse]
    .map { response =>
      response.status match {
        case Status.OK => ()
        case other => Errors.throwServerErrorException(s"Unexpected status in the http response: $other.")
      }
    }

  def findProvidedDetailsByLinkId(linkId: LinkId)(using
    request: RequestHeader
  ): Future[List[ProvidedDetails]] = httpClient
    .get(url"$baseUrl/provideddetails/linkId/${linkId.value}")
    .execute[HttpResponse]
    .map { response =>
      response.status match {
        case Status.OK => response.json.as[List[ProvidedDetails]]
        case Status.NO_CONTENT => List.empty[ProvidedDetails]
        case other => Errors.throwServerErrorException(s"Unexpected status in the http response: $other.")
      }
    }

  private val baseUrl: String = appConfig.agentRegistrationBaseUrl + "/agent-registration"
