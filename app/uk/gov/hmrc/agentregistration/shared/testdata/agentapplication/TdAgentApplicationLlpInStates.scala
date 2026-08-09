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

package uk.gov.hmrc.agentregistration.shared.testdata.agentapplication

import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.ApplicationState.GrsDataReceived
import uk.gov.hmrc.agentregistration.shared.agentdetails.*
import uk.gov.hmrc.agentregistration.shared.amls.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.audit.SessionId
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AgentDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.AmlsDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicantContactDetailsData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistration.shared.testdata.TdApplicationIdentifiers
import uk.gov.hmrc.agentregistration.shared.testdata.TdDates
import uk.gov.hmrc.auth.core.retrieve.Credentials

object TdAgentApplicationLlpInStates:

  def make(_seed: String): TdAgentApplicationLlpInStates =
    new TdAgentApplicationLlpInStates:
      override val tdApplicationIdentifiers: TdApplicationIdentifiers = TdApplicationIdentifiers.make(_seed)

trait TdAgentApplicationLlpInStates {

  def tdApplicationIdentifiers: TdApplicationIdentifiers

  val afterStarted: AgentApplicationLlp = AgentApplicationLlp(
    _id = tdApplicationIdentifiers.agentApplicationId,
    cachedSessionId = tdApplicationIdentifiers.cachedSessionId,
    applicationReference = tdApplicationIdentifiers.applicationReference,
    internalUserId = tdApplicationIdentifiers.internalUserId,
    applicantCredentials = tdApplicationIdentifiers.credentials,
    linkId = tdApplicationIdentifiers.linkId,
    groupId = tdApplicationIdentifiers.groupId,
    createdAt = TdDates.instant,
    applicationExpiresAt = Some(TdDates.instant73DaysLater),
    submittedAt = None,
    applicationState = ApplicationState.Started,
    userRole = Some(UserRole.Authorised),
    businessDetails = None,
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    globalAsaEnrolmentCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    numberOfIndividuals = None,
    hasOtherRelevantIndividuals = None,
    vrns = None,
    payeRefs = None,
    riskingOutcomeApplication = None,
    riskingOutcomeEntity = None
  )

  val afterGrsDataReceived: AgentApplicationLlp = afterStarted.copy(
    businessDetails = Some(
      BusinessDetailsLlp(
        safeId = tdApplicationIdentifiers.safeId,
        saUtr = tdApplicationIdentifiers.saUtr,
        companyProfile = tdApplicationIdentifiers.companyProfileLimitedPartnership
      )
    ),
    applicationState = GrsDataReceived
  )

  val afterRefusalToDealWithCheckPass: AgentApplicationLlp = afterGrsDataReceived.copy(
    refusalToDealWithCheckResult = Some(CheckResult.Pass)
  )

  val afterRefusalToDealWithCheckFail: AgentApplicationLlp = afterGrsDataReceived.copy(
    refusalToDealWithCheckResult = Some(CheckResult.Fail)
  )

  val afterUnifiedCustomerRegistryUpdateIdentifiers: AgentApplicationLlp = afterRefusalToDealWithCheckPass.copy(
    vrns = Some(List(tdApplicationIdentifiers.vrn)),
    payeRefs = Some(List(tdApplicationIdentifiers.payeRef))
  )

  val afterUnifiedCustomerRegistryUpdateEmptyIdentifiers: AgentApplicationLlp = afterRefusalToDealWithCheckPass.copy(
    vrns = Some(List.empty),
    payeRefs = Some(List.empty)
  )

  val afterGlobalAsaEnrolmentCheckPass: AgentApplicationLlp = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
    globalAsaEnrolmentCheckResult = Some(CheckResult.Pass)
  )

  val afterGlobalAsaEnrolmentCheckFail: AgentApplicationLlp = afterUnifiedCustomerRegistryUpdateIdentifiers.copy(
    globalAsaEnrolmentCheckResult = Some(CheckResult.Fail)
  )

  val afterContactDetailsComplete: AgentApplicationLlp = afterGlobalAsaEnrolmentCheckPass.copy(
    applicantContactDetails = Some(tdApplicationIdentifiers.applicantContactDetails),
    agentDetails = None
  )

  val afterAgentDetailsComplete: AgentApplicationLlp = afterContactDetailsComplete.copy(
    agentDetails = Some(tdApplicationIdentifiers.completeAgentDetails)
  )

  val afterAmlsComplete: AgentApplicationLlp = afterAgentDetailsComplete.copy(
    amlsDetails = Some(tdApplicationIdentifiers.completeAmlsDetails)
  )

  val afterHmrcStandardForAgentsAgreed: AgentApplicationLlp = afterAmlsComplete.copy(
    hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed
  )

  val afterZeroCompaniesHouseOfficers: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
    numberOfIndividuals = Some(
      FiveOrLessOfficers(
        numberOfCompaniesHouseOfficers = 0,
        isCompaniesHouseOfficersListCorrect = true
      )
    ),
    hasOtherRelevantIndividuals = Some(true)
  )

  val afterConfirmCompaniesHouseOfficersYes: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
    numberOfIndividuals = Some(
      tdApplicationIdentifiers.fiveOrLessCompaniesHouseOfficers
    )
  )

  val afterNumberOfConfirmCompaniesHouseOfficers: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
    numberOfIndividuals = Some(
      tdApplicationIdentifiers.sixOrMoreCompaniesHouseOfficers
    )
  )

  val afterConfirmTwoChOfficers: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
    numberOfIndividuals = Some(
      tdApplicationIdentifiers.twoCompaniesHouseOfficers
    ),
    hasOtherRelevantIndividuals = Some(false)
  )

  val afterConfirmSixChOfficers: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
    numberOfIndividuals = Some(
      tdApplicationIdentifiers.sixCompaniesHouseOfficersSelectAll
    ),
    hasOtherRelevantIndividuals = Some(false)
  )

  val afterConfirmCompaniesHouseOfficersNo: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
    numberOfIndividuals = Some(
      tdApplicationIdentifiers.fiveOrLessCompaniesHouseOfficers.copy(isCompaniesHouseOfficersListCorrect = false)
    )
  )

  val afterConfirmOtherRelevantTaxAdvisersNo: AgentApplicationLlp = afterConfirmCompaniesHouseOfficersYes.copy(
    hasOtherRelevantIndividuals = Some(false)
  )

  val afterDeclarationSubmitted: AgentApplicationLlp = afterConfirmTwoChOfficers.copy(
    applicationState = ApplicationState.SentForRisking,
    submittedAt = Some(TdDates.instant72DaysLater),
    applicationExpiresAt = None
  )

  val applicationData: ApplicationData =
    val a: AgentApplicationLlp = afterDeclarationSubmitted
    ApplicationData(
      applicationReference = tdApplicationIdentifiers.applicationReference,
      internalUserId = tdApplicationIdentifiers.internalUserId,
      applicantCredentials = tdApplicationIdentifiers.credentials,
      businessType = BusinessType.Partnership.LimitedLiabilityPartnership,
      groupId = tdApplicationIdentifiers.groupId,
      applicantContactDetails = ApplicantContactDetailsData(
        applicantName = tdApplicationIdentifiers.applicantName,
        telephoneNumber = tdApplicationIdentifiers.telephoneNumber,
        applicantEmailAddress = tdApplicationIdentifiers.applicantEmailAddress
      ),
      amlsDetails = AmlsDetailsData(
        supervisoryBody = tdApplicationIdentifiers.amlsCode,
        amlsRegistrationNumber = tdApplicationIdentifiers.amlsRegistrationNumber,
        amlsEvidence = None
      ),
      agentDetails = AgentDetailsData(
        businessName = tdApplicationIdentifiers.agentBusinessName,
        telephoneNumber = tdApplicationIdentifiers.agentTelephoneNumber,
        agentEmailAddress = tdApplicationIdentifiers.applicantEmailAddress,
        agentCorrespondenceAddress = tdApplicationIdentifiers.chroAddress
      ),
      vrns = List(tdApplicationIdentifiers.vrn),
      payeRefs = List(tdApplicationIdentifiers.payeRef),
      crn = Some(tdApplicationIdentifiers.crn),
      utr = a.getUtr,
      safeId = a.getSafeId,
      arn = None
    )

}
