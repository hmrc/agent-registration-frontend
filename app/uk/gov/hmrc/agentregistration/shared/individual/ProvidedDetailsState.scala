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

package uk.gov.hmrc.agentregistration.shared.individual

import play.api.libs.json.Format
import play.api.libs.json.JsError
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.OFormat
import play.api.libs.json.Reads
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.util.JsonConfig

sealed trait ProvidedDetailsState

object ProvidedDetailsState:

  case object Precreated
  extends ProvidedDetailsState

  case object AccessConfirmed // the applicant has confirmed they have sent the link to access this provided details record
  extends ProvidedDetailsState

  case object Started
  extends ProvidedDetailsState // in progress, user can change data

  case object Finished
  extends ProvidedDetailsState

  sealed trait ReceivedRiskingResults
  extends ProvidedDetailsState

  case object Approved
  extends ReceivedRiskingResults

  final case class FailedFixable(fixes: Seq[IndividualFix])
  extends ReceivedRiskingResults

  final case class FailedNonFixable(failures: Seq[IndividualFailure])
  extends ReceivedRiskingResults

  given Format[ProvidedDetailsState] =
    given OFormat[ProvidedDetailsState.Precreated.type] = Json.format[ProvidedDetailsState.Precreated.type]
    given OFormat[ProvidedDetailsState.AccessConfirmed.type] = Json.format[ProvidedDetailsState.AccessConfirmed.type]
    given OFormat[ProvidedDetailsState.Started.type] = Json.format[ProvidedDetailsState.Started.type]
    given OFormat[ProvidedDetailsState.Finished.type] = Json.format[ProvidedDetailsState.Finished.type]
    given OFormat[ProvidedDetailsState.Approved.type] = Json.format[ProvidedDetailsState.Approved.type]
    given OFormat[ProvidedDetailsState.FailedFixable] = Json.format[ProvidedDetailsState.FailedFixable]
    given OFormat[ProvidedDetailsState.FailedNonFixable] = Json.format[ProvidedDetailsState.FailedNonFixable]
    given JsonConfiguration = JsonConfig.jsonConfiguration

    val base: OFormat[ProvidedDetailsState] = Json.format[ProvidedDetailsState]

    val legacyStringReads: Reads[ProvidedDetailsState] = Reads {
      case JsString("Precreated") => JsSuccess(ProvidedDetailsState.Precreated)
      case JsString("AccessConfirmed") => JsSuccess(ProvidedDetailsState.AccessConfirmed)
      case JsString("Started") => JsSuccess(ProvidedDetailsState.Started)
      case JsString("Finished") => JsSuccess(ProvidedDetailsState.Finished)
      case _ => JsError("Not a legacy string ProvidedDetailsState")
    }

    OFormat(base.orElse(legacyStringReads), base)
