/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.http.HeaderNames.LOCATION
import play.api.http.Status
import play.api.i18n.Lang
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.mvc.Call
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationfrontend.config.AddressLookupConfig
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.model.addresslookup.Country
import uk.gov.hmrc.agentregistrationfrontend.model.addresslookup.GetConfirmedAddressResponse
import uk.gov.hmrc.agentregistrationfrontend.model.addresslookup.JourneyId
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.given
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AddressLookupFrontendConnector @Inject() (
  http: HttpClientV2,
  val metrics: Metrics,
  addressLookupConfig: AddressLookupConfig,
  appConfig: AppConfig
)(implicit val ec: ExecutionContext)
extends RequestAwareLogging:

  /** See https://github.com/hmrc/address-lookup-frontend?tab=readme-ov-file#initializing-a-journey
    */
  def initJourney(call: Call)(implicit
    rh: RequestHeader,
    lang: Lang
  ): Future[String] =

    val addressConfig: JsValue = Json.toJson(addressLookupConfig.createJourneyConfig(s"${call.url}"))
    val url: URL = url"${appConfig.addressLookupFrontendBaseUrl}/api/v2/init"
    http
      .post(url)
      .withBody(Json.toJson(addressConfig))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.ACCEPTED =>
            response
              .header(LOCATION)
              .getOrThrowExpectedDataMissing("Location header not set in ALF response")
          case status =>
            logger.error(s"Unexpected status from ALF: $status, response: ${response.body}")
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )

  /** See https://github.com/hmrc/address-lookup-frontend?tab=readme-ov-file#obtaining-the-confirmed-address
    */
  def getConfirmedAddress(journeyId: JourneyId)(implicit
    rh: RequestHeader
  ): Future[GetConfirmedAddressResponse] =
    val url: URL = url"${appConfig.addressLookupFrontendBaseUrl}/api/confirmed?id=${journeyId.value}"
    http
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => (response.json \ "address").as[GetConfirmedAddressResponse]
          case status =>
            logger.error(s"Unexpected status from ALF: $status, response: ${response.body}, ALF JourneyId: ${journeyId.value}")
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )

  private given Reads[GetConfirmedAddressResponse] =
    given Reads[Country] = Json.reads[Country]
    Json.reads[GetConfirmedAddressResponse]
