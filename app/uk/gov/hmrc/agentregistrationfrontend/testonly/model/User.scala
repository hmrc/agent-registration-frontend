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

import play.api.libs.json.*
import play.api.mvc.PathBindable
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory
import uk.gov.hmrc.agentregistration.shared.util.ValueClassBinder
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.User.AdditionalInformation
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.User.EnrolmentKey

import java.time.LocalDate

/** This represents User at agents-external-stubs https://github.com/hmrc/agents-external-stubs/blob/main/app/uk/gov/hmrc/agentsexternalstubs/models/User.scala
  */
final case class User(
  userId: UserId,
  planetId: Option[PlanetId],
  groupId: Option[String] = None,
  confidenceLevel: Option[Int] = None,
  credentialStrength: Option[String] = None,
  credentialRole: Option[String] = None,
  nino: Option[Nino] = None,
  assignedPrincipalEnrolments: Seq[EnrolmentKey] = Seq.empty,
  assignedDelegatedEnrolments: Seq[EnrolmentKey] = Seq.empty,
  name: Option[String] = None,
  dateOfBirth: Option[LocalDate] = None,
  isNonCompliant: Option[Boolean] = None,
  complianceIssues: Option[Seq[String]] = None,
  recordIds: Seq[String] = Seq.empty,
  address: Option[User.Address] = None,
  additionalInformation: Option[AdditionalInformation] = None,
  strideRoles: Seq[String] = Seq.empty,
  deceased: Option[Boolean] = None,
  utr: Option[Utr] = None
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

final case class UserId(value: String)

object UserId:

  def make(agentApplicationId: AgentApplicationId) = UserId(s"applicant_${agentApplicationId.value}")

  def make(individualProvidedDetailsId: IndividualProvidedDetailsId) = UserId(s"individual_${individualProvidedDetailsId.value}")

  given format: Format[UserId] = JsonFormatsFactory.makeValueClassFormat
  given pathBindable: PathBindable[UserId] = ValueClassBinder.valueClassBinder[UserId](_.value)

final case class PlanetId(value: String)

object PlanetId:

  def make(agentApplicationId: AgentApplicationId) = PlanetId(s"MMTAR_${agentApplicationId.value}")
  given format: Format[PlanetId] = JsonFormatsFactory.makeValueClassFormat
  given pathBindable: PathBindable[PlanetId] = ValueClassBinder.valueClassBinder[PlanetId](_.value)
