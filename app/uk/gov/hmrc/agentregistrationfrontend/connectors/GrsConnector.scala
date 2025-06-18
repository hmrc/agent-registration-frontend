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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.models.GrsJourneyConfig
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GrsConnector @Inject()(httpClient: HttpClientV2,
                             appConfig: AppConfig)
                            (implicit ec: ExecutionContext) {

  // there are 4 GRS services handling different entities, the 4th is minor-entity-identification and likely not relevant to agents
  // each service handles multiple business entity types (party types), have only set up basic ones here
  // the config interfaces are identical between the services to handling with one generic connector
  // the exception is sole-trader-identification-frontend supports a optFullNamePageLabel field for the name question page

  def createJourney(journeyConfig: GrsJourneyConfig, partyType: String)
                   (implicit hc: HeaderCarrier): Future[String] = {
    val url = partyType match {
      case "sole-trader" => appConfig.soleTraderJourneyUrl
      case "limited-company" => appConfig.limitedCompanyJourneyUrl
      case "general-partnership" => appConfig.generalPartnershipJourneyUrl
      case _ => throw new InternalServerException(s"Party type $partyType is not a supported party type")
    }

    httpClient
      .post(url"$url")
      .withBody(Json.toJson(journeyConfig))
      .execute[HttpResponse]
      .map {
        case response@HttpResponse(CREATED, _, _) =>
          val journeyStartUrl = (response.json \ "journeyStartUrl").as[String]
          journeyStartUrl
        case response =>
          throw new Exception(s"Invalid response from GRS create journey for $partyType: Status: ${response.status} Body: ${response.body}")
      }
  }

  // skips sautr check, purely for collecting user details
  // can be used to collect individual details for director of limited company, or the transactor themselves
  def createIndividualJourney(journeyConfig: GrsJourneyConfig)
                             (implicit hc: HeaderCarrier): Future[String] = {
    val url = appConfig.individualJourneyUrl

    httpClient
      .post(url"$url")
      .withBody(Json.toJson(journeyConfig))
      .execute[HttpResponse]
      .map {
        case response@HttpResponse(CREATED, _, _) =>
          val journeyStartUrl = (response.json \ "journeyStartUrl").as[String]
          journeyStartUrl
        case response =>
          throw new Exception(s"Invalid response from GRS create journey for individual: Status: ${response.status} Body: ${response.body}")
      }
  }

  def getJourneyDetails(partyType: Option[String], journeyid: String)
                       (implicit hc: HeaderCarrier): Future[JsValue] = {
    val url = partyType match {
      case None => appConfig.retrieveSoleTraderIdDetailsUrl(journeyid)
      case Some("sole-trader") => appConfig.retrieveSoleTraderIdDetailsUrl(journeyid)
      case Some("limited-company") => appConfig.retrieveIncorpIdDetailsUrl(journeyid)
      case Some("general-partnership") => appConfig.retrievePartnershipIdDetailsUrl(journeyid)
      case _ => throw new Exception(s"Party type $partyType is not a supported party type")
    }

    httpClient
      .get(url"$url")
      .execute[HttpResponse]
      .map(_.json)
  }
}
