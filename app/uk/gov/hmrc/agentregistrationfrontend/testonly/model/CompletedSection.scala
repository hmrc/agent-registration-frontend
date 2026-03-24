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

import play.api.mvc.PathBindable
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.util.PathBindableFactory
import uk.gov.hmrc.agentregistration.shared.util.SealedObjects
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdTestOnly

sealed trait CompletedSection:

  def sectionName: String
  def businessType: BusinessType
  def displayOrder: Int
  def agentApplication: AgentApplication
  def maybeIndividualProvidedDetailsList: Option[List[IndividualProvidedDetails]] = None

object CompletedSection:

  sealed trait CompletedSectionLlp
  extends CompletedSection:
    override final def businessType: BusinessType = BusinessType.Partnership.LimitedLiabilityPartnership

  object CompletedSectionLlp:

    case object LlpAboutYourBusiness
    extends CompletedSectionLlp:

      override def sectionName: String = "About your business"
      override def displayOrder: Int = 1
      override def agentApplication: AgentApplication = TdTestOnly.llp.twoChOfficers.agentApplicationLlp.afterCompaniesHouseStatusCheckPass

    case object LlpApplicantContactDetails
    extends CompletedSectionLlp:

      override def sectionName: String = "Applicant Contact Details"
      override def displayOrder: Int = 2
      override def agentApplication: AgentApplication = TdTestOnly.llp.twoChOfficers.agentApplicationLlp.afterContactDetailsComplete

    case object LlpAgentServicesAccountDetails
    extends CompletedSectionLlp:

      override def sectionName: String = "Agent services account details"
      override def displayOrder: Int = 3
      override def agentApplication: AgentApplication = TdTestOnly.llp.twoChOfficers.agentApplicationLlp.afterAgentDetailsComplete

    case object LlpAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionLlp:

      override def sectionName: String = "Anti-money laundering supervision details"
      override def displayOrder: Int = 4
      override def agentApplication: AgentApplication = TdTestOnly.llp.twoChOfficers.agentApplicationLlp.afterAmlsComplete

    case object LlpHmrcStandardForAgents
    extends CompletedSectionLlp:

      override def sectionName: String = "HMRC standard for agents"
      override def displayOrder: Int = 5
      override def agentApplication: AgentApplication = TdTestOnly.llp.twoChOfficers.agentApplicationLlp.afterHmrcStandardForAgentsAgreed

    case object LlpPartnersAndOtherRelevantTaxAdvisers2
    extends CompletedSectionLlp:

      override def sectionName: String = "Members and other relevant tax advisers (2)"
      override def displayOrder: Int = 6
      override def agentApplication: AgentApplication = TdTestOnly.llp.twoChOfficers.agentApplicationLlp.afterConfirmTwoChOfficers
      override def maybeIndividualProvidedDetailsList: Option[List[IndividualProvidedDetails]] = Some(
        List.fill(2)(TdTestOnly.llp.twoChOfficers.providedDetails.precreated)
      )

    case object LlpPartnersAndOtherRelevantTaxAdvisers6
    extends CompletedSectionLlp:

      override def sectionName: String = "Members and other relevant tax advisers (6)"
      override def displayOrder: Int = 7
      override def agentApplication: AgentApplication = TdTestOnly.llp.sixChOfficers.agentApplicationLlp.afterConfirmSixChOfficers
      override def maybeIndividualProvidedDetailsList: Option[List[IndividualProvidedDetails]] = Some(
        List.fill(6)(TdTestOnly.llp.sixChOfficers.providedDetails.precreated)
      )

    case object LlpDeclaration
    extends CompletedSectionLlp:

      override def sectionName: String = "Declaration"
      override def displayOrder: Int = 8
      override def agentApplication: AgentApplication = TdTestOnly.llp.twoChOfficers.agentApplicationLlp.afterDeclarationSubmitted
      override def maybeIndividualProvidedDetailsList: Option[List[IndividualProvidedDetails]] = Some(
        List.fill(2)(TdTestOnly.llp.twoChOfficers.providedDetails.afterProvidedDetailsConfirmed)
      )

    val values: Seq[CompletedSectionLlp] = SealedObjects.all[CompletedSectionLlp]

  sealed trait CompletedSectionSoleTrader
  extends CompletedSection:
    override def businessType: BusinessType = BusinessType.SoleTrader

  object CompletedSectionSoleTrader:

    case object SoleTraderAboutYourBusiness
    extends CompletedSectionSoleTrader:

      override def sectionName: String = "About your business"
      override def displayOrder: Int = 1
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationSoleTrader.afterGrsDataReceived

    case object SoleTraderApplicantContactDetails
    extends CompletedSectionSoleTrader:

      override def sectionName: String = "Applicant Contact Details"
      override def displayOrder: Int = 2
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationSoleTrader.afterContactDetailsComplete

    case object SoleTraderAgentServicesAccountDetails
    extends CompletedSectionSoleTrader:

      override def sectionName: String = "Agent services account details"
      override def displayOrder: Int = 3
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationSoleTrader.afterAgentDetailsComplete

    case object SoleTraderAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionSoleTrader:

      override def sectionName: String = "Anti-money laundering supervision details"
      override def displayOrder: Int = 4
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationSoleTrader.afterAmlsComplete

    case object SoleTraderHmrcStandardForAgents
    extends CompletedSectionSoleTrader:

      override def sectionName: String = "HMRC standard for agents"
      override def displayOrder: Int = 5
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationSoleTrader.afterHmrcStandardForAgentsAgreed

    case object SoleTraderDeclaration
    extends CompletedSectionSoleTrader:

      override def sectionName: String = "Declaration"
      override def displayOrder: Int = 6
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationSoleTrader.afterDeclarationSubmitted

    val values: Seq[CompletedSectionSoleTrader] = SealedObjects.all[CompletedSectionSoleTrader]

  sealed trait CompletedSectionGeneralPartnership
  extends CompletedSection:
    override def businessType: BusinessType = BusinessType.Partnership.GeneralPartnership

  object CompletedSectionGeneralPartnership:

    case object GeneralPartnershipAboutYourBusiness
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "About your business"
      override def displayOrder: Int = 1
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationGeneralPartnership.afterRefusalToDealWithCheckPass

    case object GeneralPartnershipApplicantContactDetails
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "Applicant Contact Details"
      override def displayOrder: Int = 2
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationGeneralPartnership.afterContactDetailsComplete

    case object GeneralPartnershipAgentServicesAccountDetails
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "Agent services account details"
      override def displayOrder: Int = 3
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationGeneralPartnership.afterAgentDetailsComplete

    case object GeneralPartnershipAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "Anti-money laundering supervision details"
      override def displayOrder: Int = 4
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationGeneralPartnership.afterAmlsComplete

    case object GeneralPartnershipHmrcStandardForAgents
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "HMRC standard for agents"
      override def displayOrder: Int = 5
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationGeneralPartnership.afterHmrcStandardForAgentsAgreed

    case object GeneralPartnershipPartnersAndOtherRelevantTaxAdvisers2
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "Partners and other relevant tax advisers (2)"
      override def displayOrder: Int = 6
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationGeneralPartnership.afterConfirmTwoIndividuals
      override def maybeIndividualProvidedDetailsList: Option[List[IndividualProvidedDetails]] = Some(
        List.fill(2)(TdTestOnly.providedDetails.precreated)
      )

    case object GeneralPartnershipPartnersAndOtherRelevantTaxAdvisers6
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "Partners and other relevant tax advisers (6)"
      override def displayOrder: Int = 7
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationGeneralPartnership.afterConfirmSixIndividuals
      override def maybeIndividualProvidedDetailsList: Option[List[IndividualProvidedDetails]] = Some(
        List.fill(6)(TdTestOnly.providedDetails.precreated)
      )

    case object GeneralPartnershipDeclaration
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "Declaration"
      override def displayOrder: Int = 8
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationGeneralPartnership.afterDeclarationSubmittedAndTwoIndividualFinished
      override def maybeIndividualProvidedDetailsList: Option[List[IndividualProvidedDetails]] = Some(
        List.fill(2)(TdTestOnly.providedDetails.afterProvidedDetailsConfirmed)
      )

    val values: Seq[CompletedSectionGeneralPartnership] = SealedObjects.all[CompletedSectionGeneralPartnership]

  sealed trait CompletedSectionScottishPartnership
  extends CompletedSection:
    override def businessType: BusinessType = BusinessType.Partnership.ScottishPartnership

  object CompletedSectionScottishPartnership:

    case object ScottishPartnershipAboutYourBusiness
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "About your business"
      override def displayOrder: Int = 1
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationScottishPartnership.afterRefusalToDealWithCheckPass

    case object ScottishPartnershipApplicantContactDetails
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "Applicant Contact Details"
      override def displayOrder: Int = 2
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationScottishPartnership.afterContactDetailsComplete

    case object ScottishPartnershipAgentServicesAccountDetails
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "Agent services account details"
      override def displayOrder: Int = 3
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationScottishPartnership.afterAgentDetailsComplete

    case object ScottishPartnershipAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "Anti-money laundering supervision details"
      override def displayOrder: Int = 4
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationScottishPartnership.afterAmlsComplete

    case object ScottishPartnershipHmrcStandardForAgents
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "HMRC standard for agents"
      override def displayOrder: Int = 5
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationScottishPartnership.afterHmrcStandardForAgentsAgreed

    case object ScottishPartnershipDeclaration
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "Declaration"
      override def displayOrder: Int = 7
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationScottishPartnership.afterDeclarationSubmitted

    val values: Seq[CompletedSectionScottishPartnership] = SealedObjects.all[CompletedSectionScottishPartnership]

  sealed trait CompletedSectionLimitedCompany
  extends CompletedSection:
    override def businessType: BusinessType = BusinessType.LimitedCompany

  object CompletedSectionLimitedCompany:

    case object LimitedCompanyAboutYourBusiness
    extends CompletedSectionLimitedCompany:

      override def sectionName: String = "About your business"
      override def displayOrder: Int = 1
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationLimitedCompany.afterRefusalToDealWithCheckPass

    case object LimitedCompanyApplicantContactDetails
    extends CompletedSectionLimitedCompany:

      override def sectionName: String = "Applicant Contact Details"
      override def displayOrder: Int = 2
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationLimitedCompany.afterContactDetailsComplete

    case object LimitedCompanyAgentServicesAccountDetails
    extends CompletedSectionLimitedCompany:

      override def sectionName: String = "Agent services account details"
      override def displayOrder: Int = 3
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationLimitedCompany.afterAgentDetailsComplete

    case object LimitedCompanyAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionLimitedCompany:

      override def sectionName: String = "Anti-money laundering supervision details"
      override def displayOrder: Int = 4
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationLimitedCompany.afterAmlsComplete

    case object LimitedCompanyHmrcStandardForAgents
    extends CompletedSectionLimitedCompany:

      override def sectionName: String = "HMRC standard for agents"
      override def displayOrder: Int = 5
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationLimitedCompany.afterHmrcStandardForAgentsAgreed

    case object LimitedCompanyDeclaration
    extends CompletedSectionLimitedCompany:

      override def sectionName: String = "Declaration"
      override def displayOrder: Int = 6
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationLimitedCompany.afterDeclarationSubmitted

    val values: Seq[CompletedSectionLimitedCompany] = SealedObjects.all[CompletedSectionLimitedCompany]

  sealed trait CompletedSectionLimitedPartnership
  extends CompletedSection:
    override def businessType: BusinessType = BusinessType.Partnership.LimitedPartnership

  object CompletedSectionLimitedPartnership:

    case object LimitedPartnershipAboutYourBusiness
    extends CompletedSectionLimitedPartnership:

      override def sectionName: String = "About your business"
      override def displayOrder: Int = 1
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationLimitedPartnership.afterRefusalToDealWithCheckPass

    case object LimitedPartnershipApplicantContactDetails
    extends CompletedSectionLimitedPartnership:

      override def sectionName: String = "Applicant Contact Details"
      override def displayOrder: Int = 2
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationLimitedPartnership.afterContactDetailsComplete

    case object LimitedPartnershipAgentServicesAccountDetails
    extends CompletedSectionLimitedPartnership:

      override def sectionName: String = "Agent services account details"
      override def displayOrder: Int = 3
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationLimitedPartnership.afterAgentDetailsComplete

    case object LimitedPartnershipAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionLimitedPartnership:

      override def sectionName: String = "Anti-money laundering supervision details"
      override def displayOrder: Int = 4
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationLimitedPartnership.afterAmlsComplete

    case object LimitedPartnershipHmrcStandardForAgents
    extends CompletedSectionLimitedPartnership:

      override def sectionName: String = "HMRC standard for agents"
      override def displayOrder: Int = 5
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationLimitedPartnership.afterHmrcStandardForAgentsAgreed

    case object LimitedPartnershipDeclaration
    extends CompletedSectionLimitedPartnership:

      override def sectionName: String = "Declaration"
      override def displayOrder: Int = 6
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationLimitedPartnership.afterDeclarationSubmitted

    val values: Seq[CompletedSectionLimitedPartnership] = SealedObjects.all[CompletedSectionLimitedPartnership]

  sealed trait CompletedSectionScottishLimitedPartnership
  extends CompletedSection:
    override def businessType: BusinessType = BusinessType.Partnership.ScottishLimitedPartnership

  object CompletedSectionScottishLimitedPartnership:

    case object ScottishLimitedPartnershipAboutYourBusiness
    extends CompletedSectionScottishLimitedPartnership:

      override def sectionName: String = "About your business"
      override def displayOrder: Int = 1
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationScottishLimitedPartnership.afterRefusalToDealWithCheckPass

    case object ScottishLimitedPartnershipApplicantContactDetails
    extends CompletedSectionScottishLimitedPartnership:

      override def sectionName: String = "Applicant Contact Details"
      override def displayOrder: Int = 2
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationScottishLimitedPartnership.afterContactDetailsComplete

    case object ScottishLimitedPartnershipAgentServicesAccountDetails
    extends CompletedSectionScottishLimitedPartnership:

      override def sectionName: String = "Agent services account details"
      override def displayOrder: Int = 3
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationScottishLimitedPartnership.afterAgentDetailsComplete

    case object ScottishLimitedPartnershipAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionScottishLimitedPartnership:

      override def sectionName: String = "Anti-money laundering supervision details"
      override def displayOrder: Int = 4
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationScottishLimitedPartnership.afterAmlsComplete

    case object ScottishLimitedPartnershipHmrcStandardForAgents
    extends CompletedSectionScottishLimitedPartnership:

      override def sectionName: String = "HMRC standard for agents"
      override def displayOrder: Int = 5
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationScottishLimitedPartnership.afterHmrcStandardForAgentsAgreed

    case object ScottishLimitedPartnershipDeclaration
    extends CompletedSectionScottishLimitedPartnership:

      override def sectionName: String = "Declaration"
      override def displayOrder: Int = 6
      override def agentApplication: AgentApplication = TdTestOnly.agentApplicationScottishLimitedPartnership.afterDeclarationSubmitted

    val values: Seq[CompletedSectionScottishLimitedPartnership] = SealedObjects.all[CompletedSectionScottishLimitedPartnership]

  val values: Seq[CompletedSection] = SealedObjects.all[CompletedSection]

  given PathBindable[CompletedSection] = PathBindableFactory.makeSealedObjectPathBindable[CompletedSection]
