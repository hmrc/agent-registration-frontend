/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.shared.contactdetails

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.EmailAddress

final case class ApplicantEmailAddress(
  emailToVerify: EmailAddress,
  verifiedEmail: Option[EmailAddress] = None
):
  val isVerified: Boolean = verifiedEmail.contains(emailToVerify)

object ApplicantEmailAddress:

  given Format[ApplicantEmailAddress] = Json.format[ApplicantEmailAddress]
