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

package uk.gov.hmrc.agentregistrationfrontend.model.application

import play.api.libs.functional.syntax.*
import play.api.libs.json.Format

final case class SessionId(value: String)

object SessionId:

  // bridge between two domain worlds
  given Conversion[uk.gov.hmrc.http.SessionId, SessionId] with
    def apply(sid: uk.gov.hmrc.http.SessionId): SessionId = SessionId(sid.value)

  given Conversion[Option[uk.gov.hmrc.http.SessionId], Option[SessionId]] with
    def apply(sid: Option[uk.gov.hmrc.http.SessionId]): Option[SessionId] = sid.map(x => summon[Conversion[uk.gov.hmrc.http.SessionId, SessionId]](x))

  given Format[SessionId] = summon[Format[String]].inmap(SessionId(_), _.value)
