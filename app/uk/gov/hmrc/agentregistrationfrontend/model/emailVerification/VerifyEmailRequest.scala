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

package uk.gov.hmrc.agentregistrationfrontend.model.emailVerification

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class VerifyEmailRequest(
  credId: String,
  continueUrl: String,
  origin: String,
  deskproServiceName: Option[String],
  accessibilityStatementUrl: String,
  email: Option[Email],
  lang: Option[String],
  backUrl: Option[String],
  pageTitle: Option[String],
  useNewGovUkServiceNavigation: Option[Boolean] = Some(true) // default to match our setting
)

object VerifyEmailRequest:
  given Writes[VerifyEmailRequest] = Json.writes[VerifyEmailRequest]
