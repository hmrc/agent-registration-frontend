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

package uk.gov.hmrc.agentregistrationfrontend.journey

import play.api.libs.json.OFormat
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationfrontend.model.{Nino, Utr}
import uk.gov.hmrc.agentregistrationfrontend.util.Errors

import java.time.{Clock, Instant}


case class Journey(
                    private val _id: JourneyId,
                    createdAt: Instant,
                    sessionId: SessionId,
                    journeyState: JourneyState,
                    nino: Option[Nino],
                    utr: Option[Utr]
                  ) {


  /* derived stuff: */
  val id: JourneyId = _id
  val journeyId: JourneyId = _id
  val lastUpdated: Instant = Instant.now(Clock.systemUTC())
  val hasFinished: Boolean = journeyState match {
    case JourneyStates.Finished => true
    case _ => false
  }

  val isInProgress: Boolean = !hasFinished

  def getUtr(implicit request: RequestHeader): Nino = nino.getOrElse(Errors.throwServerErrorException(s"Expected 'utr' to be defined but it was None [${journeyId.toString}] "))

  def getNino(implicit request: RequestHeader): Nino = nino.getOrElse(Errors.throwServerErrorException(s"Expected 'nino' to be defined but it was None [${journeyId.toString}] "))

}

object Journey {
  implicit val format: OFormat[Journey] = JourneyFormat.format
}