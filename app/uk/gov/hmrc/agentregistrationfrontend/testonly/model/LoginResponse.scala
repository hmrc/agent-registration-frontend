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

package uk.gov.hmrc.agentregistrationfrontend.testonly.model

import play.api.mvc.request.Cell
import play.api.mvc.request.RequestAttrKey
import play.api.mvc.Headers
import play.api.mvc.Request
import play.api.mvc.Session
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing

final case class LoginResponse(
  authorization: String,
  sessionId: String,
  planetId: PlanetId,
  userId: UserId,
  location: String
):

  def asHeaderCarrier: HeaderCarrier = HeaderCarrier(
    sessionId = Some(uk.gov.hmrc.http.SessionId(sessionId)),
    authorization = Some(uk.gov.hmrc.http.Authorization(asBearer(authorization)))
  )

  /** Updates given request with auth and session id
    */
  def refineRequest[CT](request: Request[CT]) =
    val session: Session =
      request
        .session
        + (SessionKeys.authToken -> asBearer(authorization))
        + (SessionKeys.sessionId -> sessionId)

    request
      .withHeaders(asHeaders)
      .addAttr(RequestAttrKey.Session, Cell(session))

  def asHeaders: Headers = Headers(
    HeaderNames.xSessionId -> sessionId,
    HeaderNames.authorisation -> asBearer(authorization)
  )

  private def asBearer(authToken: String): String =
    val trimmed = authToken.trim
    if trimmed.toLowerCase.startsWith("bearer ")
    then trimmed
    else s"Bearer $trimmed"

object LoginResponse:

  def from(response: HttpResponse): LoginResponse = LoginResponse(
    authorization = headerOne(response, HeaderNames.authorisation),
    sessionId = headerOne(response, "X-Session-ID"),
    planetId = PlanetId(headerOne(response, "X-Planet-ID")),
    userId = UserId(headerOne(response, "X-User-ID")),
    location = headerOne(response, play.api.http.HeaderNames.LOCATION)
  )

  private def headerOne(
    response: HttpResponse,
    name: String
  ): String = response
    .header(name)
    .getOrThrowExpectedDataMissing(s"$name")
