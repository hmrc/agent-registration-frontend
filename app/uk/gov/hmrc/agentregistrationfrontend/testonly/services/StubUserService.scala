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

import play.api.mvc.Request
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistrationfrontend.testonly.connectors.AgentsExternalStubsConnector
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.User.EnrolmentKey
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.hc
import uk.gov.hmrc.http.Authorization
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.User
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.SignInRequest

import java.util.UUID

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
final class StubUserService @Inject() (
  agentsExternalStubsConnector: AgentsExternalStubsConnector
)(implicit ec: ExecutionContext) {

  def createAndLoginAgent[A](using request: Request[A]): Future[HeaderCarrier] = {
    for {
      initialLoginResponse <- agentsExternalStubsConnector.signIn()

      hc = headerCarrierFrom(
        sessionId = initialLoginResponse.sessionId,
        authorization = initialLoginResponse.authorization
      )
      given HeaderCarrier = hc

      deletedUserId <- agentsExternalStubsConnector.removeUser(initialLoginResponse.userId)

      agentUserId <- createAgentUser()

      loginResponse <- agentsExternalStubsConnector.signIn(
        SignInRequest(
          userId = Some(agentUserId),
          planetId = Some(initialLoginResponse.planetId)
        )
      )

    } yield headerCarrierFrom(
      sessionId = loginResponse.sessionId,
      authorization = loginResponse.authorization
    )

  }

  def createAgentUser()(using hc: HeaderCarrier): Future[String] =
    val user = User(
      userId = UUID.randomUUID().toString,
      assignedPrincipalEnrolments = Seq.empty[EnrolmentKey]
    )
    agentsExternalStubsConnector.createUser(user, affinityGroup = Some("Agent"))

  private def headerCarrierFrom(
    sessionId: String,
    authorization: String
  ): HeaderCarrier = HeaderCarrier(
    sessionId = Some(SessionId(sessionId)),
    authorization = Some(Authorization(asBearer(authorization)))
  )

  private def asBearer(authToken: String): String =
    val trimmed = authToken.trim
    if trimmed.toLowerCase.startsWith("bearer ") then trimmed
    else s"Bearer $trimmed"

}
