/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.testonly.model

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsResult
import play.api.libs.json.JsString
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.User.AdditionalInformation
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.User.EnrolmentKey
import uk.gov.hmrc.agentregistration.shared.Nino
import java.time.LocalDate

final case class User(
  userId: String,
  groupId: Option[String] = None,
  confidenceLevel: Option[Int] = None,
  credentialStrength: Option[String] = None,
  credentialRole: Option[String] = None,
  nino: Option[Nino] = None,
  assignedPrincipalEnrolments: Seq[EnrolmentKey] = Seq.empty,
  assignedDelegatedEnrolments: Seq[EnrolmentKey] = Seq.empty,
  name: Option[String] = None,
  dateOfBirth: Option[LocalDate] = None,
  planetId: Option[String] = None,
  isNonCompliant: Option[Boolean] = None,
  complianceIssues: Option[Seq[String]] = None,
  recordIds: Seq[String] = Seq.empty,
  address: Option[User.Address] = None,
  additionalInformation: Option[AdditionalInformation] = None,
  strideRoles: Seq[String] = Seq.empty,
  deceased: Option[Boolean] = None,
  utr: Option[String] = None
)

object User:

  case class Address(
    line1: Option[String] = None,
    line2: Option[String] = None,
    postcode: Option[String] = None,
    countryCode: Option[String] = Some("GB")
  )
  object Address:
    given format: Format[Address] = Json.format[Address]

  case class AdditionalInformation(vatRegistrationDate: Option[LocalDate] = None)
  object AdditionalInformation:
    given format: Format[AdditionalInformation] = Json.format[AdditionalInformation]

  case class EnrolmentKey(tag: String)
  object EnrolmentKey:
    given format: Format[EnrolmentKey] =
      new Format[EnrolmentKey]:
        override def reads(json: JsValue): JsResult[EnrolmentKey] = json.validate[String].map(EnrolmentKey(_))
        override def writes(o: EnrolmentKey): JsValue = JsString(o.tag)

  given format: Format[User] = Json.format[User]
