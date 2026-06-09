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

package uk.gov.hmrc.agentregistration.shared

import play.api.libs.json.Format
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import java.time.LocalDate

sealed trait RiskingProgress

case object SentForRisking
extends RiskingProgress

case object SubmittedForRisking
extends RiskingProgress

sealed trait CompletedRisking
extends RiskingProgress:
  def riskingCompletedDate: LocalDate

final case class Approved(override val riskingCompletedDate: LocalDate)
extends CompletedRisking

final case class FailedFixable(
  fixes: Seq[EntityFix],
  override val riskingCompletedDate: LocalDate,
  correctiveActionExpiryDate: Option[LocalDate]
)
extends CompletedRisking

final case class FailedNonFixable(
  failures: Seq[EntityFailure],
  override val riskingCompletedDate: LocalDate
)
extends CompletedRisking

enum ApplicationState:

  case Started
  case GrsDataReceived
  case SentForRisking

object ApplicationState:

  given Format[ApplicationState] = JsonFormatsFactory.makeEnumFormat[ApplicationState]

  extension (as: ApplicationState)
    def sentForRisking: Boolean = as === ApplicationState.SentForRisking
