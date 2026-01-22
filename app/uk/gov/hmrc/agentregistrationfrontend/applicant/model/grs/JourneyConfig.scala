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

package uk.gov.hmrc.agentregistrationfrontend.applicant.model.grs

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.OFormat

/** Configuration for initiating a Grs journey. This configuration is sent as the request body when creating a Grs journey.
  * @note
  *   The name JourneyConfig matches the terminology used in Grs services otherwise it would be named CreatingGrsJourneyRequest
  */
final case class JourneyConfig(
  continueUrl: String,
  deskProServiceId: String,
  signOutUrl: String,
  accessibilityUrl: String,
  regime: String,
  businessVerificationCheck: Boolean,
  labels: Option[JourneyLabels] = None
)

object JourneyConfig:
  given Format[JourneyConfig] = Json.format[JourneyConfig]
