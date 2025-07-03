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
import uk.gov.hmrc.agentregistrationfrontend.model.{Nino, Utr}
import uk.gov.hmrc.agentregistrationfrontend.util.Errors

import java.time.{Clock, Instant}


/**
 * The application data submitted by the user.
 */
final case class Application(
                              private val _id: ApplicationId,
                              createdAt: Instant,
                              sessionId: SessionId,
                              applicationState: ApplicationState,
                              nino: Option[Nino],
                              utr: Option[Utr]
                  ) {

  /* derived stuff: */
  val id: ApplicationId = _id
  val applicationId: ApplicationId = _id
  val lastUpdated: Instant = Instant.now(Clock.systemUTC())
  val hasFinished: Boolean = applicationState match {
    case ApplicationStates.Submitted => true
    case _ => false
  }

  val isInProgress: Boolean = !hasFinished

  def getUtr(implicit request: RequestHeader): Nino = nino.getOrElse(Errors.throwServerErrorException(s"Expected 'utr' to be defined but it was None [${applicationId.toString}] "))

  def getNino(implicit request: RequestHeader): Nino = nino.getOrElse(Errors.throwServerErrorException(s"Expected 'nino' to be defined but it was None [${applicationId.toString}] "))

}

object Application {
  implicit val format: OFormat[Application] = ApplicationFormat.format
}