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

package uk.gov.hmrc.agentregistrationfrontend.config

import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.util.EnumExtensions.*
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyId
import uk.gov.hmrc.agentregistrationfrontend.testOnly.controllers.routes as testRoutes

import javax.inject.Inject
import javax.inject.Singleton
import scala.annotation.nowarn

@Singleton
class GrsConfig @Inject() (appConfig: AppConfig):

  def grsJourneyCallbackUrl(businessType: BusinessType) =
    s"${appConfig.thisFrontendBaseUrl}/agent-registration/apply/grs-callback/${businessType.toStringHyphenated}"

  val enableGrsStub: Boolean = appConfig.enableGrsStub
  val deskProServiceId: String = appConfig.contactFrontendId
  val accessibilityUrl: String = appConfig.accessibilityStatementPath
  val regime: String = "VATC" // TODO placeholder

  def createJourneyUrl(businessType: BusinessType): String =
    val grsUrl: String =
      businessType match {
        case BusinessType.SoleTrader => s"${appConfig.soleTraderIdBaseUrl}/sole-trader-identification/api/sole-trader-journey"
        case BusinessType.LimitedCompany => s"${appConfig.incorpIdBaseUrl}/incorporated-entity-identification/api/limited-company-journey"
        case BusinessType.GeneralPartnership => s"${appConfig.partnershipIdBaseUrl}/partnership-identification/api/general-partnership-journey"
        case BusinessType.LimitedLiabilityPartnership =>
          s"${appConfig.partnershipIdBaseUrl}/partnership-identification/api/limited-liability-partnership-journey"
        case BusinessType.ScottishPartnership => s"${appConfig.partnershipIdBaseUrl}/partnership-identification/api/scottish-partnership-journey"
        case BusinessType.ScottishLimitedPartnership => s"${appConfig.partnershipIdBaseUrl}/partnership-identification/api/scottish-limited-partnership-journey"
        case BusinessType.LimitedPartnership =>
      }
      s"${appConfig.partnershipIdBaseUrl}/partnership-identification/api/limited-partnership-journey"
    val stubUrl: String = s"${appConfig.selfBaseUrl}${testRoutes.GrsStubController.setupGrsJourney(businessType).url}"
    if enableGrsStub then stubUrl else grsUrl

  def retrieveJourneyDataUrl(
    businessType: BusinessType,
    journeyId: JourneyId
  ): String =
    val grsUrl =
      businessType match
        case BusinessType.SoleTrader => s"${appConfig.soleTraderIdBaseUrl}/sole-trader-identification/api/journey/${journeyId.value}"
        case BusinessType.LimitedCompany => s"${appConfig.incorpIdBaseUrl}/incorporated-entity-identification/api/journey/${journeyId.value}"
        case _: BusinessType.Partnership => s"${appConfig.partnershipIdBaseUrl}/partnership-identification/api/journey/${journeyId.value}"
      : @nowarn( /*scala3 bug?*/ "msg=Unreachable case")

    val stubUrl: String = s"${appConfig.selfBaseUrl}${testRoutes.GrsStubController.retrieveGrsData(journeyId).url}"
    if enableGrsStub then stubUrl else grsUrl
