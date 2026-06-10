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

import play.api.libs.json.JsError
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.OFormat
import play.api.libs.json.Reads
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import java.time.LocalDate

sealed trait ApplicationState

object ApplicationState:

  case object Started
  extends ApplicationState

  case object GrsDataReceived // InProgress, user performs tasks from the task list
  extends ApplicationState

  // set by FE when Application and Individuals were sent to agent-registration-risking
  case object SentForRisking //
  extends ApplicationState

  // set by risking when agent-registration-risking submitted Application and Individuals to minerva
  case object RiskingInProgress
  extends ApplicationState

  // Set by agent-registration-risking when minerva responded with risking results and the overall risking status is known as well failures for application and individuals
  sealed trait RiskingCompleted
  extends ApplicationState:
    def riskingCompletedDate: LocalDate

  // Overall status of the application is approved.
  // The entity and all individuals passed risking screening.
  final case class Approved(override val riskingCompletedDate: LocalDate)
  extends RiskingCompleted

//Overall status of the application is failed with fixable failures.
// Entity and individuals either passed passed risking screening or failed with fixable failures
// if 'fixes' are empty there are no fixing tasks for this application (only for individuals)
final case class FailedFixable(
  fixes: Seq[EntityFix],
  override val riskingCompletedDate: LocalDate,
  correctiveActionExpiryDate: Option[LocalDate]
)
extends RiskingCompleted

//Overall status of the application is failed - this is termianal state
final case class FailedNonFixable(
  failures: Seq[EntityFailure], // if empty the Application is approved, but there exist individuals which caused overall status to be failed
  override val riskingCompletedDate: LocalDate
)
extends RiskingCompleted

given format: OFormat[ApplicationState] =
  given OFormat[ApplicationState.Started.type] = Json.format[ApplicationState.Started.type]
  given OFormat[ApplicationState.GrsDataReceived.type] = Json.format[ApplicationState.GrsDataReceived.type]
  given OFormat[ApplicationState.SentForRisking.type] = Json.format[ApplicationState.SentForRisking.type]
  given OFormat[ApplicationState.RiskingInProgress.type] = Json.format[ApplicationState.RiskingInProgress.type]
  given OFormat[ApplicationState.Approved] = Json.format[ApplicationState.Approved]
  given OFormat[ApplicationState.FailedFixable] = Json.format[ApplicationState.FailedFixable]
  given OFormat[ApplicationState.FailedNonFixable] = Json.format[ApplicationState.FailedNonFixable]

  given JsonConfiguration = JsonConfig.jsonConfiguration

  val base: OFormat[ApplicationState] = Json.format[ApplicationState]

  val legacyStringReads: Reads[ApplicationState] = Reads {
    case JsString("Started") => JsSuccess(Started)
    case JsString("GrsDataReceived") => JsSuccess(GrsDataReceived)
    case JsString("SentForRisking") => JsSuccess(SentForRisking)
    case _ => JsError("Not a legacy string ApplicationState")
  }

  OFormat(base.orElse(legacyStringReads), base)

extension (as: ApplicationState)
  def sentForRisking: Boolean = as === ApplicationState.SentForRisking
