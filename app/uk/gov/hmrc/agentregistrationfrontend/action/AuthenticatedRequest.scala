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

package uk.gov.hmrc.agentregistrationfrontend.action

import play.api.mvc.Request
import play.api.mvc.WrappedRequest
import uk.gov.hmrc.agentregistrationfrontend.model.Utr
import uk.gov.hmrc.agentregistrationfrontend.model.application.SessionId
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.auth.core.retrieve.Credentials

final class AuthenticatedRequest[A](
  val request: Request[A],
  val enrolments: Enrolments,
  val utr: Option[Utr],
  val credentials: Option[Credentials],
  val sessionId: SessionId
)
extends WrappedRequest[A](request):

  lazy val hasActiveSaEnrolment: Boolean = enrolments.enrolments.exists(e => e.key == "IR-SA" && e.isActivated)
