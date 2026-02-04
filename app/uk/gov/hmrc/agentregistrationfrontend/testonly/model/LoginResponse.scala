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

import play.api.http.HeaderNames
import uk.gov.hmrc.http.HttpResponse

final case class LoginResponse(
  authorization: String,
  sessionId: String,
  planetId: String,
  userId: String,
  location: String
)

object LoginResponse:

  def from(response: HttpResponse): LoginResponse = LoginResponse(
    authorization = headerOne(response, HeaderNames.AUTHORIZATION),
    sessionId = headerOne(response, "X-Session-ID"),
    planetId = headerOne(response, "X-Planet-ID"),
    userId = headerOne(response, "X-User-ID"),
    location = headerOne(response, HeaderNames.LOCATION)
  )

  private def headerOne(
    response: HttpResponse,
    name: String
  ): String = response
    .header(name)
    .getOrElse {
      throw new RuntimeException("\"Missing required header: $name")
    }
