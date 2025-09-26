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

import play.api.http.HeaderNames
import play.api.http.Status.CREATED
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.config.GrsConfig
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyData
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyConfig
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyStartUrl
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.given
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps

import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import scala.annotation.nowarn
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.chaining.scalaUtilChainingOps
import sttp.model.Uri
import sttp.model.UriInterpolator
import uk.gov.hmrc.agentregistrationfrontend.testOnly.controllers.routes as testRoutes

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
)(using ExecutionContext):

  def createJourney(
    journeyConfig: JourneyConfig,
    businessType: BusinessType
  )(using request: AuthorisedRequest[?]): Future[JourneyStartUrl] = httpClient
    .post(url"${grsConfig.createJourneyUrl(businessType)}")
    .withBody(Json.toJson(journeyConfig))
    .execute[HttpResponse]
    .map {
      case response @ HttpResponse(CREATED, _, _) =>
        val journeyStartUrl: JourneyStartUrl = (response.json \ "journeyStartUrl").as[String].pipe(JourneyStartUrl.apply)
        journeyStartUrl
      // TODO dedicated exception which accepts context and standardizes error message (including agentApplication id, requestId, etc)
      case response => throw new Exception(s"Unexpected response from GRS create journey for $businessType: Status: ${response.status} Body: ${response.body}")
    }

  def getJourneyData(
    businessType: BusinessType,
    journeyId: String
  )(using request: AuthorisedRequest[?]): Future[JourneyData] =
    // HC override is needed because the GRS stub data is stored in the session cookie
    // By default the header carrier drops the session cookie
    given headerCarrier: HeaderCarrier =
      if grsConfig.enableGrsStub then hc.copy(extraHeaders = hc.headers(Seq(HeaderNames.COOKIE)))
      else hc
    httpClient
      .get(url"${grsConfig.retrieveJourneyDataUrl(businessType, journeyId)}")
      .execute[JourneyData]
