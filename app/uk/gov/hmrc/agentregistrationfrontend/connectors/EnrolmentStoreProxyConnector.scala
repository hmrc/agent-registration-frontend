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
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.EnrolmentStoreProxyConnector.PrincipalGroupsAllocatedToArn.PrincipalGroupId
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
          case Status.OK => (response.json \ "enrolments").as[List[EnrolmentStoreProxyConnector.Enrolment]]
          case Status.NO_CONTENT => List[EnrolmentStoreProxyConnector.Enrolment]()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed query for EnrolmentsAllocatedToGroup for $groupId")

  /** ES1: Query groups who have an allocated enrolment
    * https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=GGWRLS&title=ES1+-+Query+groups+who+have+an+allocated+enrolment
    */
  def queryPrincipleGroupsAllocatedToArn(
    agentReferenceNumber: String
  )(using RequestHeader): Future[Option[EnrolmentStoreProxyConnector.PrincipalGroupsAllocatedToArn]] =
    val url: URL = url"$baseUrl/enrolment-store/enrolments/HMRC-AS-AGENT~AgentReferenceNumber~$agentReferenceNumber/groups?type=principal"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.OK => Some(response.json.as[EnrolmentStoreProxyConnector.PrincipalGroupsAllocatedToArn])
          case Status.NO_CONTENT => None
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed query for queryGroupsAllocatedToArn for agentReferenceNumber $agentReferenceNumber")

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

  final case class PrincipalGroupsAllocatedToArn(
    principalGroupIds: List[PrincipalGroupId]
  )

  object PrincipalGroupsAllocatedToArn:

    val empty: PrincipalGroupsAllocatedToArn = PrincipalGroupsAllocatedToArn(
      principalGroupIds = List.empty
    )

    given Reads[PrincipalGroupsAllocatedToArn] = (__ \ "principalGroupIds")
      .readWithDefault[List[PrincipalGroupId]](List.empty)
      .map(PrincipalGroupsAllocatedToArn.apply)

    final case class PrincipalGroupId(
      value: String
    )

    object PrincipalGroupId:
      given Reads[PrincipalGroupId] = JsonFormatsFactory.makeValueClassFormat
