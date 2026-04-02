/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.testonly.services

import play.api.http.Status.CONFLICT
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import sttp.model.HeaderNames.Authorization
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistrationfrontend.testonly.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.LoginResponse
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.PlanetId
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.User.EnrolmentKey
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.hc
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.SignInRequest
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.User
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.UserId
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object StubUserService:

  extension (r: Result)
    def addToSession(loginResponse: LoginResponse)(using request: RequestHeader): Result = r.addingToSession(
      SessionKeys.authToken -> loginResponse.authorization,
      SessionKeys.sessionId -> loginResponse.sessionId
    )

@Singleton
final class StubUserService @Inject() (
  agentsExternalStubsConnector: AgentsExternalStubsConnector
)(implicit ec: ExecutionContext)
extends RequestAwareLogging:

//  def createAndLoginAgent(using request: Request[AnyContent]): Future[LoginResponse] =
//    for
//      user: User <- createUserApplicant()
//      loginResponse <- signIn(user)
//    yield loginResponse

  def signIn(user: User): Future[LoginResponse] = agentsExternalStubsConnector.signIn(SignInRequest(
    userId = Some(user.userId),
    planetId = user.planetId
  ))

  def createUserApplicant(
    userId: UserId,
    planetId: PlanetId
  ): Future[User] =
    val user: User = User(
      userId = userId,
      planetId = planetId
    )
    agentsExternalStubsConnector.createUser(
      user = user,
      affinityGroup = Some(AffinityGroup.Agent)
    ).map(_ => user)

  def createIndividualUser(
    userId: UserId,
    planetId: PlanetId,
    deceased: Boolean = false,
    maybeName: Option[String] = None,
    maybeNino: Option[Nino] = None,
    maybeUtr: Option[Utr] = None
  )(using
    request: RequestHeader
  ): Future[User] =
    val user = User(
      userId = userId,
      planetId = planetId,
      nino = maybeNino,
      assignedPrincipalEnrolments = Seq(EnrolmentKey("HMRC-MTD-IT")),
      deceased = Some(deceased),
      name = maybeName,
      utr = maybeUtr
    )

    agentsExternalStubsConnector.createUser(
      user = user,
      affinityGroup = Some(AffinityGroup.Individual)
    )
      .map(_ => user)

  def findUser(
    userId: UserId,
    planetId: PlanetId
  )(using RequestHeader): Future[Option[User]] = agentsExternalStubsConnector.findUser(
    userId = userId,
    planetId = planetId
  )
