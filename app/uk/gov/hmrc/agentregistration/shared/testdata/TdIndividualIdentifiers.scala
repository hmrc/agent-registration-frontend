/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.shared.testdata

import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.agentdetails.*
import uk.gov.hmrc.agentregistration.shared.amls.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.amls.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.amls.AmlsSupervisoryBodyCode
import uk.gov.hmrc.agentregistration.shared.audit.SessionId
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.businessdetails.FullName
import uk.gov.hmrc.agentregistration.shared.companieshouse.ChroAddress
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseDateOfBirth
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseNameQuery
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficer
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers
import uk.gov.hmrc.agentregistration.shared.individual.*
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficerRole.LlpMember
import uk.gov.hmrc.auth.core.ConfidenceLevel

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object TdIndividualIdentifiers:

  def make(
    _seed: String,
    _agentApplicationId: AgentApplicationId
  ): TdIndividualIdentifiers =
    new TdIndividualIdentifiers:
      override def seed: String = _seed
      override def agentApplicationId: AgentApplicationId = _agentApplicationId

trait TdIndividualIdentifiers:

  def seed: String = ""
  def internalUserId: InternalUserId = InternalUserId(s"internal-user-id-12345-$seed")
  def agentApplicationId: AgentApplicationId = AgentApplicationId(s"agent-application-id-$seed")
  def personReference: PersonReference = PersonReference(s"PREF_$seed")

  def confidenceLevel250: ConfidenceLevel = ConfidenceLevel.L250
  def confidenceLevel50: ConfidenceLevel = ConfidenceLevel.L50

  def saUtr: SaUtr = SaUtr(s"7774567895$seed")
  def individualName = IndividualName(s"Test Name$seed")
  def nino = Nino(s"AB777456C$seed")
  def ninoFromAuth = IndividualNino.FromAuth(nino)
  def ninoProvided = IndividualNino.Provided(nino)
  def saUtrFromAuth = IndividualSaUtr.FromAuth(saUtr)
  def saUtrFromCitizenDetails = IndividualSaUtr.FromCitizenDetails(saUtr)
  def saUtrProvided = IndividualSaUtr.Provided(saUtr)

  def vrn = Vrn(s"123456789-$seed")
  def payeRef = PayeRef(s"123/AB12345-$seed")

  def dateOfBirth: LocalDate = LocalDate.of(2000, 1, 1)
  def dateOfBirthFromCitizenDetails: IndividualDateOfBirth.FromCitizensDetails = IndividualDateOfBirth.FromCitizensDetails(dateOfBirth)
  def dateOfBirthProvided = IndividualDateOfBirth.Provided(dateOfBirth)
  def fullName: FullName = FullName(firstName = s"ST Name$seed", lastName = s"ST Lastname$seed")

  def individualEmailAddress: EmailAddress = EmailAddress(s"member$seed@test.com")

  def telephoneNumber: TelephoneNumber = TelephoneNumber(s"(+44) 10794554342$seed")
  def individualDateOfBirth: LocalDate = LocalDate.of(1980, 1, 1)
  def individualVerifiedEmailAddress = IndividualVerifiedEmailAddress(individualEmailAddress, isVerified = true)
  def individualProvidedDetailsId: IndividualProvidedDetailsId = IndividualProvidedDetailsId(s"individual-provided-details-id-12345-$seed")
