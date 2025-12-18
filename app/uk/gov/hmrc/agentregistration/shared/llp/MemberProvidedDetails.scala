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

package uk.gov.hmrc.agentregistration.shared.llp

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseMatch
import uk.gov.hmrc.agentregistration.shared.llp.ProvidedDetailsState.Finished
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistration.shared.util.Errors.*
import java.time.Instant

/** Member provided details for Limited Liability Partnership (Llp). This final case class represents the data entered by a user for approving as an Llp.
  */
final case class MemberProvidedDetails(
  _id: MemberProvidedDetailsId,
  internalUserId: InternalUserId,
  createdAt: Instant,
  providedDetailsState: ProvidedDetailsState,
  agentApplicationId: AgentApplicationId,
  companiesHouseMatch: Option[CompaniesHouseMatch] = None,
  telephoneNumber: Option[TelephoneNumber] = None,
  emailAddress: Option[MemberVerifiedEmailAddress] = None,
  memberNino: Option[MemberNino] = None,
  memberSaUtr: Option[MemberSaUtr] = None,
  hmrcStandardForAgentsAgreed: StateOfAgreement = StateOfAgreement.NotSet,
  hasApprovedApplication: Option[Boolean] = None
):

  val memberProvidedDetailsId: MemberProvidedDetailsId = _id

  private def required[T](
    value: Option[T],
    missingMessage: => String
  ): T = value.getOrThrowExpectedDataMissing(missingMessage)

  val hasFinished: Boolean = providedDetailsState === Finished
  val isInProgress: Boolean = !hasFinished

  def getCompaniesHouseMatch: CompaniesHouseMatch = required(companiesHouseMatch, "Companies house query is missing for member provided details")

  def getEmailAddress: MemberVerifiedEmailAddress = required(emailAddress, "Email address is missing")

  def getTelephoneNumber: TelephoneNumber = required(telephoneNumber, "Telephone number is missing")

  def isUserProvidedNino: Boolean = memberNino.exists {
    case _: UserProvidedNino => true
    case _ => false
  }

  def isUserProvidedSaUtr: Boolean = memberSaUtr.exists {
    case _: UserProvidedSaUtr => true
    case _ => false
  }

  def hasNino: Boolean = memberNino.exists {
    case MemberNino.Provided(_) | MemberNino.FromAuth(_) => true
    case MemberNino.NotProvided => false
  }

  def getNinoString: String = required(memberNino.flatMap(ninoValue), "Nino is missing")

  def hasSaUtr: Boolean = memberSaUtr.exists {
    case MemberSaUtr.Provided(_) | MemberSaUtr.FromAuth(_) | MemberSaUtr.FromCitizenDetails(_) => true
    case MemberSaUtr.NotProvided => false
  }

  def getSaUtrString: String = required(memberSaUtr.flatMap(saUtrValue), "SaUtr is missing")

  def getOfficerName: String =
    val officerName =
      for
        ch <- companiesHouseMatch
        officer <- ch.companiesHouseOfficer
      yield officer.name

    required(officerName, "Companies house officer name is missing")

  private def saUtrValue(saUtr: MemberSaUtr): Option[String] =
    saUtr match
      case MemberSaUtr.Provided(v) => Some(v.value)
      case MemberSaUtr.FromAuth(v) => Some(v.value)
      case MemberSaUtr.FromCitizenDetails(v) => Some(v.value)
      case MemberSaUtr.NotProvided => None

  private def ninoValue(nino: MemberNino): Option[String] =
    nino match
      case MemberNino.Provided(v) => Some(v.value)
      case MemberNino.FromAuth(v) => Some(v.value)
      case MemberNino.NotProvided => None

object MemberProvidedDetails:
  given format: OFormat[MemberProvidedDetails] = Json.format[MemberProvidedDetails]
