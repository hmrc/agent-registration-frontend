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

import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistrationfrontend.applicant.action.AuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.grs.JourneyConfig
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.grs.JourneyData
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.grs.JourneyId
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.grs.JourneyStartUrl
import uk.gov.hmrc.agentregistrationfrontend.config.GrsConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.util.chaining.scalaUtilChainingOps

/** A connector that integrates with multiple Grs microservices sharing similar endpoints and data structures in their request and response bodies.
  *
  * The Grs microservices family include:
  *   - sole-trader-identification-frontend
  *   - incorporated-entity-identification-frontend
  *   - partnership-identification-frontend
  *   - minor-entity-identification-frontend
  */
@Singleton
class GrsConnector @Inject() (
  httpClient: HttpClientV2,
  grsConfig: GrsConfig
)(using ExecutionContext)
extends Connector:

  def createJourney(
    journeyConfig: JourneyConfig,
    businessType: BusinessType
  )(using AuthorisedRequest[?]): Future[JourneyStartUrl] =
    val url: URL = url"${grsConfig.createJourneyUrl(businessType)}"
    httpClient
      .post(url)
      .withBody(Json.toJson(journeyConfig))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.CREATED => (response.json \ "journeyStartUrl").as[String].pipe(JourneyStartUrl.apply)
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to create Grs Journey for $businessType")

  def getJourneyData(
    businessType: BusinessType,
    journeyId: JourneyId
  )(using AuthorisedRequest[?]): Future[JourneyData] =
    // HC override is needed because the GRS stub data is stored in the session cookie
    // By default the header carrier drops the session cookie
    given headerCarrier: HeaderCarrier =
      if grsConfig.enableGrsStub then hc.copy(extraHeaders = hc.headers(Seq(HeaderNames.COOKIE)))
      else hc

    val url: URL = url"${grsConfig.retrieveJourneyDataUrl(businessType, journeyId)}"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if is2xx(status) => response.json.as[JourneyData]
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to retrieve Grs Journey for $journeyId, $businessType")
