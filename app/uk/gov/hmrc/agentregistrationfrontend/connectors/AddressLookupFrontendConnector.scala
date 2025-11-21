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
import play.api.i18n.Lang
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationfrontend.config.AddressLookupConfig
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.given
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.agentregistration.shared.AddressLookupFrontendAddress
import uk.gov.hmrc.agentregistrationfrontend.model.addresslookup.JourneyId
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NoStackTrace

@Singleton
class AddressLookupFrontendConnector @Inject() (
  http: HttpClientV2,
  val metrics: Metrics,
  addressLookupConfig: AddressLookupConfig,
  appConfig: AppConfig
)(implicit val ec: ExecutionContext) {

  def initJourney(call: Call)(implicit
    rh: RequestHeader,
    ec: ExecutionContext,
    lang: Lang
  ): Future[String] =
    val addressConfig = Json.toJson(addressLookupConfig.createJourneyConfig(s"${call.url}"))
    http
      .post(url"$initJourneyUrl")
      .withBody(Json.toJson(addressConfig))
      .execute[HttpResponse].map: resp =>
        resp
          .header(LOCATION)
          .getOrElse(throw new ALFLocationHeaderNotSetException)

  def getAddressDetails(journeyId: JourneyId)(implicit
    rh: RequestHeader,
    ec: ExecutionContext
  ): Future[AddressLookupFrontendAddress] = http
    .get(url"${confirmJourneyUrl(journeyId)}")
    .execute[JsObject]
    .map(json => (json \ "address").as[AddressLookupFrontendAddress])

  private def confirmJourneyUrl(journeyId: JourneyId) = s"${appConfig.addressLookupFrontendBaseUrl}/api/confirmed?id=${journeyId.value}"

  private def initJourneyUrl: String = s"${appConfig.addressLookupFrontendBaseUrl}/api/v2/init"

}

class ALFLocationHeaderNotSetException
extends NoStackTrace
