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

package uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.CREATED
import play.api.http.Status.OK
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.BusinessType.*
import uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock.StubMaker
import uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock.StubMaker.HttpMethod.GET
import uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock.StubMaker.HttpMethod.POST

object GrsStubs:

  val grsSoleTraderJourneyUrl = "/grs/start/sole-trader"
  val grsLimitedCompanyJourneyUrl = "/grs/start/limited-company"
  val grsGeneralPartnershipJourneyUrl = "/grs/start/general-partnership"
  val grsLimitedLiabilityPartnershipJourneyUrl = "/grs/start/limited-liability-partnership"

  def stubCreateGrsJourney(businessType: BusinessType): StubMapping = {
    val url =
      businessType match {
        case SoleTrader => s"/sole-trader-identification/api/sole-trader-journey"
        case LimitedCompany => s"/incorporated-entity-identification/api/limited-company-journey"
        case GeneralPartnership => s"/partnership-identification/api/general-partnership-journey"
        case LimitedLiabilityPartnership => s"/partnership-identification/api/limited-liability-partnership-journey"
      }
    StubMaker.make(
      httpMethod = POST,
      urlPattern = urlMatching(url),
      responseStatus = CREATED,
      responseBody =
        Json.obj(
          "journeyStartUrl" -> (businessType match {
            case SoleTrader => grsSoleTraderJourneyUrl
            case LimitedCompany => grsLimitedCompanyJourneyUrl
            case GeneralPartnership => grsGeneralPartnershipJourneyUrl
            case LimitedLiabilityPartnership => grsLimitedLiabilityPartnershipJourneyUrl
          })
        ).toString
    )
  }

  def stubGetGrsResponse(
    businessType: BusinessType,
    journeyId: String,
    responseBody: JsValue
  ): StubMapping = {
    val url =
      businessType match {
        case SoleTrader => s"/sole-trader-identification/api/journey/$journeyId"
        case LimitedCompany => s"/incorporated-entity-identification/api/journey/$journeyId"
        case GeneralPartnership | LimitedLiabilityPartnership => s"/partnership-identification/api/journey/$journeyId"
      }
    StubMaker.make(
      httpMethod = GET,
      urlPattern = urlMatching(url),
      responseStatus = OK,
      responseBody = responseBody.toString
    )
  }
