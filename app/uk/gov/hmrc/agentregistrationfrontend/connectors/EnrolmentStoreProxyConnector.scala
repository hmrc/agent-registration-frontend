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

import play.api.libs.functional.syntax.*
import play.api.libs.json.Reads
import play.api.libs.json.__
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class EnrolmentStoreProxyConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(using
  ExecutionContext
)
extends Connector:

  /** ES3: Query Enrolments allocated to a group https://confluence.tools.tax.service.gov.uk/display/GGWRLS/ES3+-+Query+Enrolments+allocated+to+a+group
    */
  def queryEnrolmentsAllocatedToGroup(
    groupId: GroupId
  )(using RequestHeader): Future[List[EnrolmentStoreProxyConnector.Enrolment]] =
    val url: URL = url"$baseUrl/enrolment-store/groups/${groupId.value}/enrolments"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => response.json.as[List[EnrolmentStoreProxyConnector.Enrolment]]
          case Status.NO_CONTENT => List[EnrolmentStoreProxyConnector.Enrolment]()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed query for EnrolmentsAllocatedToGroup for $groupId")

  private val baseUrl: String = appConfig.enrolmentStoreProxyBaseUrl + "/enrolment-store-proxy"

object EnrolmentStoreProxyConnector:

  final case class Enrolment(
    service: String,
    state: String
  )

  object Enrolment:
    given Reads[Enrolment] =
      (
        (__ \ "service").read[String] and
          (__ \ "state").read[String]
      )(Enrolment.apply)
