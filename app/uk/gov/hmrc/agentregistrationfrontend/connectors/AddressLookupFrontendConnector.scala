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

import uk.gov.hmrc.agentregistrationfrontend.config.AddressLookupConfig
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.model.addresslookup.Country
import uk.gov.hmrc.agentregistrationfrontend.model.addresslookup.GetConfirmedAddressResponse
import uk.gov.hmrc.agentregistrationfrontend.model.addresslookup.JourneyId
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class AddressLookupFrontendConnector @Inject() (
  http: HttpClientV2,
  addressLookupConfig: AddressLookupConfig,
  appConfig: AppConfig
)(using ExecutionContext)
extends Connector:

  /** See https://github.com/hmrc/address-lookup-frontend?tab=readme-ov-file#initializing-a-journey
    */
  def initJourney(call: Call)(using RequestHeader): Future[String] =
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
              .header(HeaderNames.LOCATION)
              .getOrThrowExpectedDataMissing("Location header not set in ALF response")
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to initiate journey at GRS")

  /** See https://github.com/hmrc/address-lookup-frontend?tab=readme-ov-file#obtaining-the-confirmed-address
    */
  def getConfirmedAddress(journeyId: JourneyId)(using RequestHeader): Future[GetConfirmedAddressResponse] =
    val url: URL = url"${appConfig.addressLookupFrontendBaseUrl}/api/confirmed?id=${journeyId.value}"
    http
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => (response.json \ "address").as[GetConfirmedAddressResponse]
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(
        s"Failed to get confirmed address from ALF, JourneyId: ${journeyId.value}"
      )

  private given Reads[GetConfirmedAddressResponse] =
    given Reads[Country] = Json.reads[Country]
    Json.reads[GetConfirmedAddressResponse]
