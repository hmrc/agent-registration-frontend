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

import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import java.time.LocalDate

sealed trait ApplicationState

object ApplicationState:

  case object Started
  extends ApplicationState

  case object GrsDataReceived
  extends ApplicationState

  case object SentForRisking
  extends ApplicationState

  case object RiskingInProgress
  extends ApplicationState

  sealed trait RiskingCompleted
  extends ApplicationState:
    def riskingCompletedDate: LocalDate

  final case class Approved(override val riskingCompletedDate: LocalDate)
  extends RiskingCompleted

  final case class FailedFixable(
    fixes: Seq[EntityFix],
    override val riskingCompletedDate: LocalDate,
    correctiveActionExpiryDate: Option[LocalDate]
  )
  extends RiskingCompleted

  final case class FailedNonFixable(
    failures: Seq[EntityFailure],
    override val riskingCompletedDate: LocalDate
  )
  extends RiskingCompleted

  @scala.annotation.nowarn()
  given format: OFormat[ApplicationState] =
    given OFormat[ApplicationState.Started.type] = Json.format[ApplicationState.Started.type]
    given OFormat[ApplicationState.GrsDataReceived.type] = Json.format[ApplicationState.GrsDataReceived.type]
    given OFormat[ApplicationState.SentForRisking.type] = Json.format[ApplicationState.SentForRisking.type]
    given OFormat[ApplicationState.RiskingInProgress.type] = Json.format[ApplicationState.RiskingInProgress.type]
    given OFormat[ApplicationState.Approved] = Json.format[ApplicationState.Approved]
    given OFormat[ApplicationState.FailedFixable] = Json.format[ApplicationState.FailedFixable]
    given OFormat[ApplicationState.FailedNonFixable] = Json.format[ApplicationState.FailedNonFixable]

    given JsonConfiguration = JsonConfig.jsonConfiguration

    Json.format[ApplicationState]

  extension (as: ApplicationState)
    def sentForRisking: Boolean = as === ApplicationState.SentForRisking
