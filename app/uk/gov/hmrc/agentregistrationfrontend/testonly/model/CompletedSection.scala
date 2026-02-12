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
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.util.PathBindableFactory
import uk.gov.hmrc.agentregistration.shared.util.SealedObjects

sealed trait CompletedSection:

  def sectionName: String
  def businessType: BusinessType
  def alreadyDeveloped: Boolean = false
  def displayOrder: Int

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

    case object LlpApplicantContactDetails
    extends CompletedSectionLlp:

      override def sectionName: String = "Applicant Contact Details"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 2

    case object LlpAgentServicesAccountDetails
    extends CompletedSectionLlp:

      override def sectionName: String = "Agent services account details"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 3

    case object LlpAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionLlp:

      override def sectionName: String = "Anti-money laundering supervision details"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 4

    case object LlpHmrcStandardForAgents
    extends CompletedSectionLlp:

      override def sectionName: String = "HMRC standard for agents"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 5

    case object LlpDeclaration
    extends CompletedSectionLlp:

      override def sectionName: String = "Declaration"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 6

    val values: Seq[CompletedSectionLlp] = SealedObjects.all[CompletedSectionLlp]

  sealed trait CompletedSectionSoleTrader
  extends CompletedSection:
    override def businessType: BusinessType = BusinessType.SoleTrader

  object CompletedSectionSoleTrader:
    // example:
    case object SoleTraderAboutYourBusiness
    extends CompletedSectionSoleTrader:

      override def sectionName: String = "About your business"
      override def alreadyDeveloped: Boolean = true
      override def displayOrder: Int = 1

  sealed trait CompletedSectionGeneralPartnership
  extends CompletedSection:
    override def businessType: BusinessType = BusinessType.Partnership.GeneralPartnership

  object CompletedSectionGeneralPartnership:

    case object GeneralPartnershipAboutYourBusiness
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "About your business"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 1

    case object GeneralPartnershipApplicantContactDetails
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "Applicant Contact Details"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 2

    case object GeneralPartnershipAgentServicesAccountDetails
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "Agent services account details"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 3

    case object GeneralPartnershipAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "Anti-money laundering supervision details"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 4

    case object GeneralPartnershipHmrcStandardForAgents
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "HMRC standard for agents"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 5

    case object GeneralPartnershipDeclaration
    extends CompletedSectionGeneralPartnership:

      override def sectionName: String = "Declaration"

      override def alreadyDeveloped: Boolean = true

      override def displayOrder: Int = 6

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

    case object ScottishPartnershipApplicantContactDetails
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "Applicant Contact Details"
      override def alreadyDeveloped: Boolean = false
      override def displayOrder: Int = 2

    case object ScottishPartnershipAgentServicesAccountDetails
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "Agent services account details"
      override def alreadyDeveloped: Boolean = false
      override def displayOrder: Int = 3

    case object ScottishPartnershipAntiMoneyLaunderingSupervisionDetails
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "Anti-money laundering supervision details"
      override def alreadyDeveloped: Boolean = false
      override def displayOrder: Int = 4

    case object ScottishPartnershipHmrcStandardForAgents
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "HMRC standard for agents"
      override def alreadyDeveloped: Boolean = false
      override def displayOrder: Int = 5

    case object ScottishPartnershipDeclaration
    extends CompletedSectionScottishPartnership:

      override def sectionName: String = "Declaration"
      override def alreadyDeveloped: Boolean = false
      override def displayOrder: Int = 6

    val values: Seq[CompletedSectionScottishPartnership] = SealedObjects.all[CompletedSectionScottishPartnership]

  val values: Seq[CompletedSection] = SealedObjects.all[CompletedSection]

  given PathBindable[CompletedSection] = PathBindableFactory.makeSealedObjectPathBindable[CompletedSection]
