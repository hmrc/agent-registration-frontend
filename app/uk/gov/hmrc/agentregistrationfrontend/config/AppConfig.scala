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

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(servicesConfig: ServicesConfig, config: Configuration) {
  val welshLanguageSupportEnabled: Boolean = config.getOptional[Boolean]("features.welsh-language-support").getOrElse(false)

  val contactFormServiceIdentifier = "AGNTREG" //placeholder

  // GRS
  val soleTraderIdBaseUrl: String = baseUrl("sole-trader-identification-frontend")
  val incorpIdBaseUrl: String = baseUrl("incorporated-entity-identification-frontend")
  val partnershipIdBaseUrl: String = baseUrl("partnership-identification-frontend")

  def grsJourneyCallbackUrl(partyType: String) =
    s"${getConfString("agent-registration-frontend.external-url")}/agent-registration-frontend/grs-callback/$partyType"

  val grsRegistrantCallbackUrl =
    s"${getConfString("agent-registration-frontend.external-url")}/agent-registration-frontend/grs-callback-registrant"

  val grsTransactorCallbackUrl =
    s"${getConfString("agent-registration-frontend.external-url")}/agent-registration-frontend/grs-callback-transactor"

  //Sole trader
  val soleTraderJourneyUrl = s"$soleTraderIdBaseUrl/sole-trader-identification/api/sole-trader-journey"
  val individualJourneyUrl = s"$soleTraderIdBaseUrl/sole-trader-identification/api/individual-journey"

  def retrieveSoleTraderIdDetailsUrl(journeyId: String): String = s"$soleTraderIdBaseUrl/sole-trader-identification/api/journey/$journeyId"

  //Incorporated
  val limitedCompanyJourneyUrl = s"$incorpIdBaseUrl/incorporated-entity-identification/api/limited-company-journey"

  def retrieveIncorpIdDetailsUrl(journeyId: String): String = s"$incorpIdBaseUrl/incorporated-entity-identification/api/journey/$journeyId"

  //Partnership
  val generalPartnershipJourneyUrl = s"$partnershipIdBaseUrl/partnership-identification/api/general-partnership-journey"

  def retrievePartnershipIdDetailsUrl(journeyId: String): String = s"$partnershipIdBaseUrl/partnership-identification/api/journey/$journeyId"

  private def getString(key: String) = servicesConfig.getString(key)

  // For config contained in 'microservice.services'
  private def getConfString(key: String) =
    servicesConfig.getConfString(key, throw new RuntimeException(s"config $key not found"))

  private def baseUrl(serviceName: String) = servicesConfig.baseUrl(serviceName)
}
