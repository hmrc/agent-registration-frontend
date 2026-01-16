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

import play.api.libs.functional.syntax.*
import uk.gov.hmrc.agentregistration.shared.Crn
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseDateOfBirth
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficer
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.model.CompanyHouseStatus
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class CompaniesHouseApiProxyConnector @Inject() (
  appConfig: AppConfig,
  http: HttpClientV2
)(using ExecutionContext)
extends Connector:

  private val baseUrl: String = appConfig.companiesHouseApiProxyBaseUrl

  def getCompaniesHouseOfficers(
    crn: Crn,
    surname: String,
    isLlp: Boolean = false
  )(using
    RequestHeader
  ): Future[Seq[CompaniesHouseOfficer]] =

    val registerType: String = if isLlp then "llp_members" else "directors"
    val params: Map[String, Any] = Map(
      "surname" -> surname,
      "register_view" -> true,
      "register_type" -> registerType
    )

    val url: URL = url"$baseUrl/companies-house-api-proxy/company/${crn.value}/officers?$params"

    http
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if is2xx(status) => (response.json \ "items").as[Seq[CompaniesHouseOfficer]]
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to retrieve officers from Companies House for ${crn.value}")

  def getCompanyHouseStatus(
    crn: Crn
  )(implicit
    rh: RequestHeader
  ): Future[CompanyHouseStatus] =

    val url = url"$baseUrl/companies-house-api-proxy/company/${crn.value}"
    http
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case s if is2xx(s) => (response.json \ "company_status").as[CompanyHouseStatus]
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to retrieve company status for ${crn.value}")

  private given Reads[CompaniesHouseOfficer] =
    (
      (__ \ "name").read[String] and
        (__ \ "date_of_birth").readNullable[CompaniesHouseDateOfBirth]
    )(CompaniesHouseOfficer.apply)
