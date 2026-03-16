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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.CREATED
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.BusinessType.*
import uk.gov.hmrc.agentregistration.shared.BusinessType.Partnership.*
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyId
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyData
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker.HttpMethod.GET
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker.HttpMethod.POST

object GrsStubs:

  /** The URL which is returned to the browser upon successful creation of a GRS journey in the GRS Service
    */
  def journeyStartRedirectUrl(businessType: BusinessType): String =
    businessType match
      case SoleTrader => "/grs/start/sole-trader"
      case LimitedCompany => "/grs/start/limited-company"
      case GeneralPartnership => "/grs/start/general-partnership"
      case LimitedLiabilityPartnership => "/grs/start/limited-liability-partnership"
      case LimitedPartnership => "/grs/start/limited-partnership"
      case ScottishPartnership => "/grs/start/scottish-partnership"
      case ScottishLimitedPartnership => "/grs/start/scottish-limited-partnership"

  def stubCreateJourney(businessType: BusinessType): StubMapping = StubMaker.make(
    httpMethod = POST,
    urlPattern = wm.urlPathEqualTo(startGrsJourneyUrl(businessType)),
    responseStatus = CREATED,
    responseBody = Json.obj("journeyStartUrl" -> journeyStartRedirectUrl(businessType)).toString
  )

  def verifyCreateJourney(
    businessType: BusinessType
  ): Unit = StubMaker.verify(
    httpMethod = POST,
    urlPattern = wm.urlPathEqualTo(startGrsJourneyUrl(businessType))
  )

  def stubGetJourneyData(
    businessType: BusinessType,
    journeyId: JourneyId,
    tdAll: TdAll = TdAll.tdAll
  ): StubMapping =

    val journeyData: JourneyData =
      businessType match
        case bt @ BusinessType.Partnership.GeneralPartnership => throw NotImplementedError(s"$bt not implemented yet")
        case BusinessType.Partnership.LimitedLiabilityPartnership => tdAll.grs.llp.journeyData
        case bt @ BusinessType.Partnership.LimitedPartnership => throw NotImplementedError(s"$bt not implemented yet")
        case bt @ BusinessType.Partnership.ScottishLimitedPartnership => throw NotImplementedError(s"$bt not implemented yet")
        case bt @ BusinessType.Partnership.ScottishPartnership => throw NotImplementedError(s"$bt not implemented yet")
        case bt @ BusinessType.SoleTrader => throw NotImplementedError(s"$bt not implemented yet")
        case bt @ BusinessType.LimitedCompany => throw NotImplementedError(s"$bt not implemented yet")

    StubMaker.make(
      httpMethod = GET,
      urlPattern = wm.urlPathEqualTo(getJourneyDataUrl(businessType, journeyId)),
      responseStatus = OK,
      responseBody = Json.prettyPrint(Json.toJson(journeyData))
    )

  def verifyGetJourneyData(
    businessType: BusinessType,
    journeyId: JourneyId
  ): Unit = StubMaker.verify(
    httpMethod = GET,
    urlPattern = wm.urlPathEqualTo(getJourneyDataUrl(businessType, journeyId))
  )

  /** The url of the GrsService to retrieve Grs Journey Data
    */
  private def getJourneyDataUrl(
    businessType: BusinessType,
    journeyId: JourneyId
  ): String =
    businessType match
      case SoleTrader => s"/sole-trader-identification/api/journey/${journeyId.value}"
      case LimitedCompany => s"/incorporated-entity-identification/api/journey/${journeyId.value}"
      case GeneralPartnership | LimitedLiabilityPartnership | LimitedPartnership | ScottishLimitedPartnership | ScottishPartnership =>
        s"/partnership-identification/api/journey/${journeyId.value}"

  /** The url of the GrsService to start Grs Journey
    */
  private def startGrsJourneyUrl(businessType: BusinessType): String =
    businessType match
      case BusinessType.SoleTrader => s"/sole-trader-identification/api/sole-trader-journey"
      case BusinessType.LimitedCompany => s"/incorporated-entity-identification/api/limited-company-journey"
      case BusinessType.Partnership.GeneralPartnership => s"/partnership-identification/api/general-partnership-journey"
      case BusinessType.Partnership.LimitedLiabilityPartnership => s"/partnership-identification/api/limited-liability-partnership-journey"
      case BusinessType.Partnership.LimitedPartnership => s"/partnership-identification/api/limited-partnership-journey"
      case BusinessType.Partnership.ScottishPartnership => s"/partnership-identification/api/scottish-partnership-journey"
      case BusinessType.Partnership.ScottishLimitedPartnership => s"/partnership-identification/api/scottish-limited-partnership-journey"
