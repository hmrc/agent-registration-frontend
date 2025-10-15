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

import play.api.libs.json.Reads
import play.api.libs.functional.syntax.*
import play.api.libs.json.__
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.Crn
import uk.gov.hmrc.agentregistration.shared.contactdetails.CompaniesHouseDateOfBirth
import uk.gov.hmrc.agentregistration.shared.contactdetails.CompaniesHouseOfficer
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.given
import uk.gov.hmrc.http.HttpErrorFunctions.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class CompaniesHouseApiProxyConnector @Inject() (
  appConfig: AppConfig,
  http: HttpClientV2,
  val metrics: Metrics
)(implicit val ec: ExecutionContext)
extends RequestAwareLogging:

  val baseUrl: String = appConfig.companiesHouseApiProxyBaseUrl

  def getCompaniesHouseOfficers(
    crn: Crn,
    surname: String,
    isLlp: Boolean = false
  )(implicit
    rh: RequestHeader
  ): Future[Seq[CompaniesHouseOfficer]] =

    implicit val companiesHouseOfficerReads: Reads[CompaniesHouseOfficer] =
      (
        (__ \ "name").read[String] and
          (__ \ "date_of_birth").readNullable[CompaniesHouseDateOfBirth]
      )(CompaniesHouseOfficer.apply)

    val registerType: String = if isLlp then "llp_members" else "directors"
    val params = Map(
      "surname" -> surname,
      "register_view" -> true,
      "register_type" -> registerType
    )
    http
      .get(url"$baseUrl/companies-house-api-proxy/company/${crn.value}/officers?$params")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case s if is2xx(s) => (response.json \ "items").as[Seq[CompaniesHouseOfficer]]
          case s => throw UpstreamErrorResponse(s"Upstream error response from Companies House API Proxy: $s", s)
        }
      }
