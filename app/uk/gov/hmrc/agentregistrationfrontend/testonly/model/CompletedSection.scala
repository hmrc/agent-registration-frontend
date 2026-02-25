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
import uk.gov.hmrc.agentregistration.shared.util.PathBindableFactory
import uk.gov.hmrc.agentregistration.shared.util.SealedObjects
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData

sealed trait CompletedSection:

  def sectionName: String
  def businessType: BusinessType
  def alreadyDeveloped: Boolean = false
  def displayOrder: Int
  def appState: AgentApplication

object CompletedSection:

  sealed trait CompletedSectionLlp
  extends CompletedSection:
    override final def businessType: BusinessType = BusinessType.Partnership.LimitedLiabilityPartnership

  object CompletedSectionLlp:

    case object LlpAboutYourBusiness
    extends CompletedSectionLlp:

      override def sectionName: String = "About your business"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 1
      override def appState: AgentApplication = TestOnlyData.agentApplicationLlp.afterCompaniesHouseStatusCheckPass

    case object LlpApplicantContactDetails
    extends CompletedSectionLlp:

      override def sectionName: String = "Applicant Contact Details"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 2
      override def appState: AgentApplication = TestOnlyData.agentApplicationLlp.afterContactDetailsComplete

    case object LlpAgentServicesAccountDetails
    extends CompletedSectionLlp:

      override def sectionName: String = "Agent services account details"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 3
      override def appState: AgentApplication = TestOnlyData.agentApplicationLlp.afterAgentDetailsComplete

    case object LlpAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionLlp:

      override def sectionName: String = "Anti-money laundering supervision details"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 4
      override def appState: AgentApplication = TestOnlyData.agentApplicationLlp.afterAmlsComplete

    case object LlpHmrcStandardForAgents
    extends CompletedSectionLlp:

      override def sectionName: String = "HMRC standard for agents"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 5
      override def appState: AgentApplication = TestOnlyData.agentApplicationLlp.afterHmrcStandardForAgentsAgreed

    case object LlpDeclaration
    extends CompletedSectionLlp:

      override def sectionName: String = "Declaration"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 6
      override def appState: AgentApplication = TestOnlyData.agentApplicationLlp.afterDeclarationSubmitted

    val values: Seq[CompletedSectionLlp] = SealedObjects.all[CompletedSectionLlp]

  sealed trait CompletedSectionSoleTrader
  extends CompletedSection:
    override def businessType: BusinessType = BusinessType.SoleTrader

  object CompletedSectionSoleTrader:

    case object SoleTraderAboutYourBusiness
    extends CompletedSectionSoleTrader:

      override def sectionName: String = "About your business"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 1
      override def appState: AgentApplication = TestOnlyData.agentApplicationSoleTrader.afterGrsDataReceived

    case object SoleTraderApplicantContactDetails
    extends CompletedSectionSoleTrader:

      override def sectionName: String = "Applicant Contact Details"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 2
      override def appState: AgentApplication = TestOnlyData.agentApplicationSoleTrader.afterContactDetailsComplete

    case object SoleTraderAgentServicesAccountDetails
    extends CompletedSectionSoleTrader:

      override def sectionName: String = "Agent services account details"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 3
      override def appState: AgentApplication = TestOnlyData.agentApplicationSoleTrader.afterAgentDetailsComplete

    case object SoleTraderAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionSoleTrader:

      override def sectionName: String = "Anti-money laundering supervision details"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 4
      override def appState: AgentApplication = TestOnlyData.agentApplicationSoleTrader.afterAmlsComplete

    case object SoleTraderHmrcStandardForAgents
    extends CompletedSectionSoleTrader:

      override def sectionName: String = "HMRC standard for agents"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 5
      override def appState: AgentApplication = TestOnlyData.agentApplicationSoleTrader.afterHmrcStandardForAgentsAgreed

    case object SoleTraderDeclaration
    extends CompletedSectionSoleTrader:

      override def sectionName: String = "Declaration"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 6
      override def appState: AgentApplication = TestOnlyData.agentApplicationSoleTrader.afterDeclarationSubmitted

    val values: Seq[CompletedSectionSoleTrader] = SealedObjects.all[CompletedSectionSoleTrader]

  sealed trait CompletedSectionGeneralPartnership
  extends CompletedSection:
    override def businessType: BusinessType = BusinessType.Partnership.GeneralPartnership

  object CompletedSectionGeneralPartnership:

    case object GeneralPartnershipAboutYourBusiness
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "About your business"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 1
      override def appState: AgentApplication = TestOnlyData.agentApplicationGeneralPartnership.afterRefusalToDealWithCheckPass

    case object GeneralPartnershipApplicantContactDetails
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "Applicant Contact Details"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 2
      override def appState: AgentApplication = TestOnlyData.agentApplicationGeneralPartnership.afterContactDetailsComplete

    case object GeneralPartnershipAgentServicesAccountDetails
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "Agent services account details"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 3
      override def appState: AgentApplication = TestOnlyData.agentApplicationGeneralPartnership.afterAgentDetailsComplete

    case object GeneralPartnershipAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "Anti-money laundering supervision details"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 4
      override def appState: AgentApplication = TestOnlyData.agentApplicationGeneralPartnership.afterAmlsComplete

    case object GeneralPartnershipHmrcStandardForAgents
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "HMRC standard for agents"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 5
      override def appState: AgentApplication = TestOnlyData.agentApplicationGeneralPartnership.afterHmrcStandardForAgentsAgreed

    case object GeneralPartnershipDeclaration
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "Declaration"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 6
      override def appState: AgentApplication = TestOnlyData.agentApplicationGeneralPartnership.afterDeclarationSubmitted

    val values: Seq[CompletedSectionGeneralPartnership] = SealedObjects.all[CompletedSectionGeneralPartnership]

  sealed trait CompletedSectionScottishPartnership
  extends CompletedSection:
    override def businessType: BusinessType = BusinessType.Partnership.ScottishPartnership

  object CompletedSectionScottishPartnership:

    case object ScottishPartnershipAboutYourBusiness
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "About your business"
      override def alreadyDeveloped: Boolean = false
      override def displayOrder: Int = 1
      override def appState: AgentApplication = TestOnlyData.agentApplicationScottishPartnership.afterRefusalToDealWithCheckPass

    case object ScottishPartnershipApplicantContactDetails
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "Applicant Contact Details"
      override def alreadyDeveloped: Boolean = false
      override def displayOrder: Int = 2
      override def appState: AgentApplication = TestOnlyData.agentApplicationScottishPartnership.afterContactDetailsComplete

    case object ScottishPartnershipAgentServicesAccountDetails
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "Agent services account details"
      override def alreadyDeveloped: Boolean = false
      override def displayOrder: Int = 3
      override def appState: AgentApplication = TestOnlyData.agentApplicationScottishPartnership.afterAgentDetailsComplete

    case object ScottishPartnershipAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "Anti-money laundering supervision details"
      override def alreadyDeveloped: Boolean = false
      override def displayOrder: Int = 4
      override def appState: AgentApplication = TestOnlyData.agentApplicationScottishPartnership.afterAmlsComplete

    case object ScottishPartnershipHmrcStandardForAgents
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "HMRC standard for agents"
      override def alreadyDeveloped: Boolean = false
      override def displayOrder: Int = 5
      override def appState: AgentApplication = TestOnlyData.agentApplicationScottishPartnership.afterHmrcStandardForAgentsAgreed

    case object ScottishPartnershipDeclaration
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "Declaration"
      override def alreadyDeveloped: Boolean = false
      override def displayOrder: Int = 6
      override def appState: AgentApplication = TestOnlyData.agentApplicationScottishPartnership.afterDeclarationSubmitted

    val values: Seq[CompletedSectionScottishPartnership] = SealedObjects.all[CompletedSectionScottishPartnership]

  sealed trait CompletedSectionLimitedCompany
  extends CompletedSection:
    override def businessType: BusinessType = BusinessType.LimitedCompany

  object CompletedSectionLimitedCompany:

    case object LimitedCompanyAboutYourBusiness
    extends CompletedSectionLimitedCompany:

      override def sectionName: String = "About your business"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 1

      override def appState: AgentApplication = TestOnlyData.agentApplicationLimitedCompany.afterRefusalToDealWithCheckPass

    case object LimitedCompanyApplicantContactDetails
    extends CompletedSectionLimitedCompany:

      override def sectionName: String = "Applicant Contact Details"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 2

      override def appState: AgentApplication = TestOnlyData.agentApplicationLimitedCompany.afterContactDetailsComplete

    case object LimitedCompanyAgentServicesAccountDetails
    extends CompletedSectionLimitedCompany:

      override def sectionName: String = "Agent services account details"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 3

      override def appState: AgentApplication = TestOnlyData.agentApplicationLimitedCompany.afterAgentDetailsComplete

    case object LimitedCompanyAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionLimitedCompany:

      override def sectionName: String = "Anti-money laundering supervision details"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 4

      override def appState: AgentApplication = TestOnlyData.agentApplicationLimitedCompany.afterAmlsComplete

    case object LimitedCompanyHmrcStandardForAgents
    extends CompletedSectionLimitedCompany:

      override def sectionName: String = "HMRC standard for agents"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 5

      override def appState: AgentApplication = TestOnlyData.agentApplicationLimitedCompany.afterHmrcStandardForAgentsAgreed

    case object LimitedCompanyDeclaration
    extends CompletedSectionLimitedCompany:

      override def sectionName: String = "Declaration"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 6

      override def appState: AgentApplication = TestOnlyData.agentApplicationLimitedCompany.afterDeclarationSubmitted

    val values: Seq[CompletedSectionLimitedCompany] = SealedObjects.all[CompletedSectionLimitedCompany]

  sealed trait CompletedSectionLimitedPartnership
  extends CompletedSection:
    override def businessType: BusinessType = BusinessType.Partnership.LimitedPartnership

  object CompletedSectionLimitedPartnership:

    case object LimitedPartnershipAboutYourBusiness
    extends CompletedSectionLimitedPartnership:

      override def sectionName: String = "About your business"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 1

      override def appState: AgentApplication = TestOnlyData.agentApplicationLimitedPartnership.afterRefusalToDealWithCheckPass

    case object LimitedPartnershipApplicantContactDetails
    extends CompletedSectionLimitedPartnership:

      override def sectionName: String = "Applicant Contact Details"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 2

      override def appState: AgentApplication = TestOnlyData.agentApplicationLimitedPartnership.afterContactDetailsComplete

    case object LimitedPartnershipAgentServicesAccountDetails
    extends CompletedSectionLimitedPartnership:

      override def sectionName: String = "Agent services account details"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 3

      override def appState: AgentApplication = TestOnlyData.agentApplicationLimitedPartnership.afterAgentDetailsComplete

    case object LimitedPartnershipAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionLimitedPartnership:

      override def sectionName: String = "Anti-money laundering supervision details"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 4

      override def appState: AgentApplication = TestOnlyData.agentApplicationLimitedPartnership.afterAmlsComplete

    case object LimitedPartnershipHmrcStandardForAgents
    extends CompletedSectionLimitedPartnership:

      override def sectionName: String = "HMRC standard for agents"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 5

      override def appState: AgentApplication = TestOnlyData.agentApplicationLimitedPartnership.afterHmrcStandardForAgentsAgreed

    case object LimitedPartnershipDeclaration
    extends CompletedSectionLimitedPartnership:

      override def sectionName: String = "Declaration"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 6

      override def appState: AgentApplication = TestOnlyData.agentApplicationLimitedPartnership.afterDeclarationSubmitted

    val values: Seq[CompletedSectionLimitedPartnership] = SealedObjects.all[CompletedSectionLimitedPartnership]

  val values: Seq[CompletedSection] = SealedObjects.all[CompletedSection]

  given PathBindable[CompletedSection] = PathBindableFactory.makeSealedObjectPathBindable[CompletedSection]
