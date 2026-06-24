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

// see days-to-submit-application = 73 days in application.conf

object TdApplicationIdentifiers:

  def make(_seed: String = ""): TdApplicationIdentifiers =
    new TdApplicationIdentifiers:
      override def seed: String = _seed

trait TdApplicationIdentifiers:

  protected def seed: String = ""

  def internalUserId: InternalUserId = InternalUserId(s"internal-user-id-12345-$seed")
  def agentApplicationId: AgentApplicationId = AgentApplicationId(s"agent-application-id-12345-$seed")
  def applicationReference: ApplicationReference = ApplicationReference(s"APPREF_$seed")
  def cachedSessionId: SessionId = SessionId(s"session-id-123$seed")

  def saUtr: SaUtr = SaUtr(s"1234567895$seed")
  def ctUtr: CtUtr = CtUtr(s"2202108031$seed")
  def linkId: LinkId = LinkId(s"link-id-12345-$seed")
  def groupId: GroupId = GroupId(s"group-id-12345-$seed")
  def credentials: Credentials = Credentials(
    providerId = s"cred-id-12345-$seed",
    providerType = "GovernmentGateway"
  )

  def safeId: SafeId = SafeId(s"XA0001234512345$seed")

  def applicantEmailAddress: EmailAddress = EmailAddress(s"user$seed@test.com")

  def telephoneNumber: TelephoneNumber = TelephoneNumber(s"(+44) 10794554342$seed")
  def crn: Crn = Crn(s"1234567890$seed")
  def companyName = s"Test Company Name$seed"
  def limitedCompanyName = s"Test Company Ltd$seed"
  def limitedPartnershipName = s"Test Partnership$seed"
  def dateOfIncorporation: LocalDate = LocalDate.now().minusYears(10)

  def personReference: PersonReference = PersonReference(s"1234567890$seed")
  def applicantName: ApplicantName = ApplicantName(authorisedPersonName)
  def agentBusinessName: AgentBusinessName = AgentBusinessName(agentBusinessName = companyName, otherAgentBusinessName = None)
  def amlsCode: AmlsSupervisoryBodyCode = AmlsSupervisoryBodyCode("HMRC")
  def amlsRegistrationNumber: AmlsRegistrationNumber = AmlsRegistrationNumber(s"XAML00000123456$seed")
  def vrn = Vrn(s"123456789$seed")
  def payeRef = PayeRef(s"123/AB12345$seed")
  def trn: String = s"ST-TRN-987654321$seed"
  def agentTelephoneNumber = AgentTelephoneNumber(agentTelephoneNumber = telephoneNumber.value, otherAgentTelephoneNumber = None)

  def companyProfile: CompanyProfile = CompanyProfile(
    companyNumber = crn,
    companyName = companyName,
    dateOfIncorporation = Some(dateOfIncorporation),
    unsanitisedCHROAddress = Some(ChroAddress(
      address_line_1 = Some("23 Great Portland Street"),
      address_line_2 = Some("London"),
      postal_code = Some("W1 8LT"),
      country = Some("GB")
    ))
  )

  def companyProfileLimited: CompanyProfile = companyProfile.copy(companyName = limitedCompanyName)
  def companyProfileLimitedPartnership: CompanyProfile = companyProfile.copy(companyName = limitedPartnershipName)
  def postcode: String = "AA1 1AA"
  def authorisedPersonName: String = s"Alice Smith$seed"
  def agentVerifiedEmailAddress = AgentVerifiedEmailAddress(
    emailAddress = AgentEmailAddress(
      agentEmailAddress = applicantEmailAddress.value,
      otherAgentEmailAddress = None
    ),
    isVerified = true
  )
  def applicantContactDetails: ApplicantContactDetails = ApplicantContactDetails(
    applicantName = applicantName,
    telephoneNumber = Some(telephoneNumber),
    applicantEmailAddress = Some(ApplicantEmailAddress(
      emailAddress = applicantEmailAddress,
      isVerified = true
    ))
  )
  def completeAgentDetails: AgentDetails = AgentDetails(
    agentCorrespondenceAddress = Some(chroAddress),
    telephoneNumber = Some(agentTelephoneNumber),
    agentEmailAddress = Some(agentVerifiedEmailAddress),
    businessName = agentBusinessName
  )

  def completeAmlsDetails: AmlsDetails = AmlsDetails(
    supervisoryBody = amlsCode,
    amlsRegistrationNumber = Some(amlsRegistrationNumber),
    amlsEvidence = None
  )

  def bprPrimaryTelephoneNumber: String = s"(+44) 78714743399$seed"
  def newTelephoneNumber: String = s"+44 (0) 7000000000$seed"
  def bprEmailAddress: String = s"bpr$seed@example.com"
  def newEmailAddress: String = s"new$seed@example.com"
  def chroAddress: AgentCorrespondenceAddress = AgentCorrespondenceAddress(
    addressLine1 = "23 Great Portland Street",
    addressLine2 = Some("London"),
    postalCode = Some("W1 8LT"),
    countryCode = "GB"
  )
  def bprRegisteredAddress: DesBusinessAddress = DesBusinessAddress(
    addressLine1 = "Registered Line 1",
    addressLine2 = Some("Registered Line 2"),
    addressLine3 = None,
    addressLine4 = None,
    postalCode = Some("AB1 2CD"),
    countryCode = "GB"
  )
  def llpNameQuery = CompaniesHouseNameQuery(
    firstName = "Jane",
    lastName = "Leadenhall-Lane"
  )
  def companiesHouseOfficer = CompaniesHouseOfficer(
    name = "Taylor Leadenhall-Lane",
    dateOfBirth = Some(CompaniesHouseDateOfBirth(
      day = Some(12),
      month = 11,
      year = 1990
    )),
    resignedOn = None,
    officerRole = Some(LlpMember),
    identification = None
  )

  def businessPartnerRecordResponse: BusinessPartnerRecordResponse = BusinessPartnerRecordResponse(
    organisationName = Some("Test Company Name"),
    agentReferenceNumber = None,
    individualName = None,
    address = bprRegisteredAddress,
    primaryPhoneNumber = Some(bprPrimaryTelephoneNumber),
    emailAddress = Some(bprEmailAddress),
    isAnAsaAgent = false
  )

//  def businessPartnerRecordResponseSoleTrader: BusinessPartnerRecordResponse = BusinessPartnerRecordResponse(
//    organisationName = None,
//    agentReferenceNumber = None,
//    individualName = Some(individualName.value),
//    address = bprRegisteredAddress,
//    primaryPhoneNumber = Some(bprPrimaryTelephoneNumber),
//    emailAddress = Some(bprEmailAddress),
//    isAnAsaAgent = false
//  )

  def fiveOrFewerKeyIndividuals: FiveOrLess = FiveOrLess(
    numberOfKeyIndividuals = 3
  )

  def fiveOrLessCompaniesHouseOfficers: FiveOrLessOfficers = FiveOrLessOfficers(
    numberOfCompaniesHouseOfficers = 1,
    isCompaniesHouseOfficersListCorrect = true
  )

  def twoCompaniesHouseOfficers: FiveOrLessOfficers = FiveOrLessOfficers(
    numberOfCompaniesHouseOfficers = 2,
    isCompaniesHouseOfficersListCorrect = true
  )

  def sixOrMoreKeyIndividuals: SixOrMore = SixOrMore(
    numberOfKeyIndividualsResponsibleForTaxMatters = 3
  )

  def sixOrMoreCompaniesHouseOfficers: SixOrMoreOfficers = SixOrMoreOfficers(
    numberOfCompaniesHouseOfficers = 6,
    numberOfOfficersResponsibleForTaxMatters = 4
  )

  def sixCompaniesHouseOfficersSelectAll: SixOrMoreOfficers = SixOrMoreOfficers(
    numberOfCompaniesHouseOfficers = 6,
    numberOfOfficersResponsibleForTaxMatters = 6
  )

  /** This is a list of individual names that we currently have stubbed in companies house, We need to use this list for fast forward links to ensure the names
    * match the names we get from companies house stub
    */
  val individualNamesStubbedInCompaniesHouse: List[IndividualName] = List(
    IndividualName("Steve Austin"),
    IndividualName("Beverly Hills"),
    IndividualName("Pauline Austin"),
    IndividualName("Justine Hills"),
    IndividualName("Steve Palmer"),
    IndividualName("Sandra Hills")
  )

  def getIndividualName(
    index: Int
  ): IndividualName = individualNamesStubbedInCompaniesHouse.lift(index).getOrElse(throw new RuntimeException(s"Missing individual name for index $index"))

  def ucrIdentifiers: UcrIdentifiers = UcrIdentifiers(
    vrns = List(vrn),
    payeRefs = List(payeRef)
  )

  def emptyUcrIdentifiers: UcrIdentifiers = UcrIdentifiers(
    vrns = List.empty,
    payeRefs = List.empty
  )

  def arn: Arn = Arn(s"TARN0000001$seed")
