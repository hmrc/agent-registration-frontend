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

import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.model.citizendetails.CitizenDetails
import uk.gov.hmrc.agentregistrationfrontend.model.llp.DesignatoryDetailsResponse
import uk.gov.hmrc.http.client.HttpClientV2

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class CitizenDetailsConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2
)(using ExecutionContext)
extends Connector:

  private val baseUrl: String = appConfig.citizenDetailsBaseUrl

  def getCitizenDetails(
    nino: Nino
  )(using RequestHeader): Future[CitizenDetails] = httpClient
    .get(url"${baseUrl}/citizen-details/nino/${nino.value}")
    .execute[CitizenDetails]

  /** See https://github.com/hmrc/citizen-details?tab=readme-ov-file#get-citizen-detailsninodesignatory-details
    */
  def getDesignatoryDetails(
    nino: Nino
  )(using RequestHeader): Future[DesignatoryDetailsResponse] =
    val url: URL = url"$baseUrl/citizen-details/${nino.value}/designatory-details"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case s if is2xx(s) => response.json.as[DesignatoryDetailsResponse]
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to get designatory details")

  private given Reads[DesignatoryDetailsResponse] =
    for
      deceased <- (__ \ "person" \ "deceased")
        .readNullable[Boolean]
        .map(_.getOrElse(false))
    yield DesignatoryDetailsResponse(deceased = deceased)

  private given Reads[CitizenDetails] =
    val citizenDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy")
    for
      firstName <- (__ \ "name" \ "current" \ "firstName").readNullable[String]
      lastName <- (__ \ "name" \ "current" \ "lastName").readNullable[String]
      dateOfBirth <- (__ \ "dateOfBirth")
        .readNullable[String]
        .map(_.map(date => LocalDate.parse(date, citizenDateFormatter)))
      saUtr <- (__ \ "ids" \ "sautr").readNullable[String].map(_.map(SaUtr.apply))
    yield CitizenDetails(
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      saUtr = saUtr
    )
