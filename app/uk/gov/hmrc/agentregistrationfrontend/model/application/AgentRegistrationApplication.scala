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

package uk.gov.hmrc.agentregistrationfrontend.model.application

import play.api.libs.json.OFormat
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationfrontend.model.{InternalUserId, Nino, Utr}
import uk.gov.hmrc.agentregistrationfrontend.util.Errors

import java.time.Clock
import java.time.Instant

/** Agent Registration Application.
 * This class holds the application data submitted by the user.
 */
final case class AgentRegistrationApplication(
                                               internalUserId: InternalUserId,
                                               createdAt: Instant,
                                               applicationState: ApplicationState,
                                               utr: Option[Utr]
):

  /* derived stuff: */
  val lastUpdated: Instant = Instant.now(Clock.systemUTC())
  val hasFinished: Boolean =
    applicationState match
      case ApplicationState.Submitted => true
      case _ => false

  val isInProgress: Boolean = !hasFinished

  def getUtr(using request: RequestHeader): Utr = utr.getOrElse(
    Errors.throwServerErrorException(s"Expected 'utr' to be defined but it was None [${internalUserId.toString}] ")
  )


object AgentRegistrationApplication:
  given format: OFormat[AgentRegistrationApplication] = ApplicationFormat.format
