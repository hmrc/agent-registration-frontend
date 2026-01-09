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

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.model.llp.CitizenDetails
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.given
import uk.gov.hmrc.http.HttpErrorFunctions.is2xx
import uk.gov.hmrc.http.HttpReads.Implicits.given
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class CitizenDetailsConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  val baseUrl: String = appConfig.citizenDetailsBaseUrl

  def getCitizenDetails(
    nino: Nino
  )(using rh: RequestHeader): Future[CitizenDetails] = httpClient
    .get(url"${baseUrl}/citizen-details/nino/${nino.value}")
    .execute[CitizenDetails]

  def isDeceased(
    nino: Nino
  )(using rh: RequestHeader): Future[Boolean] =
    val url = url"$baseUrl/citizen-details/${nino.value}/designatory-details"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map { response =>
        response.status match
          case s if is2xx(s) =>
            (response.json \ "person" \ "deceased")
              .asOpt[Boolean]
              .getOrElse(false)
          case status =>
            logger.error(s"Citizen details designatory details deceased check error for ${nino.value}; HTTP status: $status")
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      }
