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

package uk.gov.hmrc.agentregistrationfrontend.testonly.connectors

import play.api.http.Status.CONFLICT
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.BusinessPartnerRecord
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.LoginResponse
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.PlanetId
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.SignInRequest
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.User
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.UserId
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.User.EnrolmentKey
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.Connector
import play.api.libs.json.JsValue
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

/** Connector to agents external stubs service used in test environments only
  */
@Singleton
class AgentsExternalStubsConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(using
  ExecutionContext
)
extends Connector:

  def storeBusinessPartnerRecord(bpr: BusinessPartnerRecord)(using
    request: RequestHeader
  ): Future[Unit] =
    val url: URL = url"$baseUrl/records/business-partner-record"
    httpClient
      .post(url)
      .withBody(Json.toJson(bpr))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.CREATED => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )

  def createIndividualUser(
    assignedPrincipalEnrolments: Seq[String],
    deceased: Boolean = false,
    maybeName: Option[String] = None,
    maybeNino: Option[Nino] = None,
    maybeUtr: Option[Utr] = None
  )(using
    request: RequestHeader
  ): Future[Unit] =
    val user = User(
      userId = UserId(UUID.randomUUID().toString),
      planetId = PlanetId.mmtar,
      nino = maybeNino,
      assignedPrincipalEnrolments = assignedPrincipalEnrolments.map(EnrolmentKey(_)),
      deceased = Some(deceased),
      name = maybeName,
      utr = maybeUtr.map(_.value)
    )
    createUser(user, affinityGroup = Some(AffinityGroup.Individual)).recover {
      // ignore 409 errors (created user with duplicate ninos) from user stubs repo
      case e: UpstreamErrorResponse if e.statusCode === CONFLICT =>
        logger.info(s"[AgentsExternalStubsConnector][createIndividualUser] Recovered from ${e.message}")
        ()
    }

  def signIn(signInRequest: SignInRequest)(using hc: HeaderCarrier): Future[LoginResponse] =
    val url: URL = url"$baseUrl/sign-in"
    httpClient
      .post(url)
      .withBody(Json.toJson(signInRequest))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.CREATED | Status.ACCEPTED => LoginResponse.from(response)
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response,
              info = "sign-in problem"
            )

  def removeUser(userId: UserId)(using hc: HeaderCarrier): Future[Unit] =
    val url: URL = url"$baseUrl/users/${userId.value}"
    httpClient
      .delete(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.NO_CONTENT | Status.OK => ()
          case Status.NOT_FOUND => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "DELETE",
              url = url,
              status = status,
              response = response
            )

  def createUser(
    user: User,
    affinityGroup: Option[AffinityGroup] = None
  )(using hc: HeaderCarrier): Future[Unit] =
    val queryParams: Seq[(String, String)] = ("planetId" -> user.planetId.value) :: affinityGroup.map("affinityGroup" -> _.toString).toList
    val url: URL = url"$baseUrl/users?$queryParams"

    httpClient
      .post(url)
      .withBody(Json.toJson(user))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.CREATED | Status.OK => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )

  private val baseUrl: String = appConfig.agentsExternalStubsBaseUrl + "/agents-external-stubs"
