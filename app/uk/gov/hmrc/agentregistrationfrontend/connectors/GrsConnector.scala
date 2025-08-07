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

import play.api.http.Status.CREATED
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.BusinessType.GeneralPartnership
import uk.gov.hmrc.agentregistration.shared.BusinessType.LimitedCompany
import uk.gov.hmrc.agentregistration.shared.BusinessType.LimitedLiabilityPartnership
import uk.gov.hmrc.agentregistration.shared.BusinessType.SoleTrader
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.model.GrsJourneyConfig
import uk.gov.hmrc.agentregistrationfrontend.model.GrsResponse
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.given
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.http.StringContextOps

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class GrsConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(implicit ec: ExecutionContext):

  def createGrsJourney(
    journeyConfig: GrsJourneyConfig,
    businessType: BusinessType
  )(using request: AuthorisedRequest[?]): Future[String] = httpClient
    .post(url"${appConfig.grsJourneyUrl(businessType)}")
    .withBody(Json.toJson(journeyConfig))
    .execute[HttpResponse]
    .map {
      case response @ HttpResponse(CREATED, _, _) =>
        val journeyStartUrl = (response.json \ "journeyStartUrl").as[String]
        journeyStartUrl
      case response => throw new Exception(s"Unexpected response from GRS create journey for $businessType: Status: ${response.status} Body: ${response.body}")
    }

  def getGrsResponse(
    businessType: BusinessType,
    journeyId: String
  )(using request: AuthorisedRequest[?]): Future[GrsResponse] = httpClient
    .get(url"${appConfig.grsRetrieveDetailsUrl(businessType, journeyId)}")
    .execute[GrsResponse]
