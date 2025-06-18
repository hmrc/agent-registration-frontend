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

package uk.gov.hmrc.agentregistrationfrontend.services

import play.api.i18n.MessagesApi
import play.api.libs.json.JsValue
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.GrsConnector
import uk.gov.hmrc.agentregistrationfrontend.models.GrsJourneyConfig
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GrsService @Inject()(grsConnector: GrsConnector)
                          (implicit ec: ExecutionContext, appConfig: AppConfig) {

  // This should be reusable for any kind of GRS journey
  // If collecting data for sole trader partners journey config may need a different override for the name question (to refer to the Nth partner)

  def createJourney(partyType: String, isTransactor: Boolean, callBackUrl: String)
                   (implicit messagesApi: MessagesApi, hc: HeaderCarrier): Future[String] = {
    val journeyConfig = GrsJourneyConfig.createConfig(Some(partyType), isTransactor, callBackUrl)

    grsConnector.createJourney(journeyConfig, partyType)
  }

  def createIndividualJourney(isTransactor: Boolean, callBackUrl: String)
                   (implicit messagesApi: MessagesApi, hc: HeaderCarrier): Future[String] = {
    val journeyConfig = GrsJourneyConfig.createConfig(None, isTransactor, callBackUrl)

    grsConnector.createIndividualJourney(journeyConfig)
  }

  def getJourneyDetails(partyType: Option[String], clientId: String)
                       (implicit hc: HeaderCarrier): Future[JsValue] =
    grsConnector.getJourneyDetails(partyType, clientId)


}
