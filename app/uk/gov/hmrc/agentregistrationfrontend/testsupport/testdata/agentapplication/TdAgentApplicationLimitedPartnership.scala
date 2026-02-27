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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication

import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.ApplicationState.GrsDataReceived
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdGrs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication.sections.TdSectionAgentDetails
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication.sections.TdSectionAmls
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication.sections.TdSectionContactDetails

trait TdAgentApplicationLimitedPartnership {
  dependencies: (TdBase & TdSectionAmls & TdSectionContactDetails & TdGrs & TdSectionAgentDetails) =>

  object agentApplicationLimitedPartnership:

    val afterStarted: AgentApplicationLimitedPartnership = AgentApplicationLimitedPartnership(
      _id = dependencies.agentApplicationId,
      internalUserId = dependencies.internalUserId,
      linkId = dependencies.linkId,
      groupId = dependencies.groupId,
      createdAt = dependencies.nowAsInstant,
      applicationState = ApplicationState.Started,
      userRole = Some(UserRole.Authorised),
      businessDetails = None,
      applicantContactDetails = None,
      amlsDetails = None,
      agentDetails = None,
      refusalToDealWithCheckResult = None,
      companyStatusCheckResult = None,
      hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
      numberOfRequiredKeyIndividuals = None,
      hasOtherRelevantIndividuals = None
    )

    val afterGrsDataReceived: AgentApplicationLimitedPartnership = afterStarted.copy(
      businessDetails = Some(
        dependencies.grs.ltdPartnership.businessDetails
      ),
      applicationState = GrsDataReceived
    )

    val afterRefusalToDealWithCheckPass: AgentApplicationLimitedPartnership = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Pass)
    )

    val afterRefusalToDealWithCheckFail: AgentApplicationLimitedPartnership = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Fail)
    )

    val afterCompaniesHouseStatusCheckPass: AgentApplicationLimitedPartnership = afterRefusalToDealWithCheckPass.copy(
      companyStatusCheckResult = Some(CheckResult.Pass)
    )

    val afterCompaniesHouseStatusCheckFail: AgentApplicationLimitedPartnership = afterRefusalToDealWithCheckPass.copy(
      companyStatusCheckResult = Some(CheckResult.Fail)
    )

    val afterContactDetailsComplete: AgentApplicationLimitedPartnership = afterCompaniesHouseStatusCheckPass.copy(
      applicantContactDetails = Some(dependencies.applicantContactDetails),
      agentDetails = None
    )

    val afterAgentDetailsComplete: AgentApplicationLimitedPartnership = afterContactDetailsComplete.copy(
      agentDetails = Some(dependencies.completeAgentDetails)
    )

    val afterAmlsComplete: AgentApplicationLimitedPartnership = afterAgentDetailsComplete.copy(
      amlsDetails = Some(dependencies.completeAmlsDetails)
    )

    val afterHmrcStandardForAgentsAgreed: AgentApplicationLimitedPartnership = afterAmlsComplete.copy(
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed
    )

    val afterHowManyKeyIndividuals: AgentApplicationLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      numberOfRequiredKeyIndividuals = Some(
        FiveOrLess(
          numberOfKeyIndividuals = 3
        )
      )
    )

    val afterDeclarationSubmitted: AgentApplicationLimitedPartnership = afterHmrcStandardForAgentsAgreed.copy(
      applicationState = ApplicationState.Submitted
    )

    val baseForSectionAmls: AgentApplicationLimitedPartnership = afterGrsDataReceived
    protected val AgentApplicationLimitedPartnershipWithSectionAmls = new AgentApplicationWithSectionAmls(baseForSectionAmls = baseForSectionAmls)
    export AgentApplicationLimitedPartnershipWithSectionAmls.sectionAmls

    val baseForSectionContactDetails: AgentApplicationLimitedPartnership = afterGrsDataReceived
    protected val tdAgentApplicationLimitedPartnershipWithSectionContactDetails =
      new TdAgentApplicationWithSectionContactDetails(baseForSectionContactDetails = baseForSectionContactDetails)

    export tdAgentApplicationLimitedPartnershipWithSectionContactDetails.sectionContactDetails

    protected val tdAgentApplicationLimitedPartnershipWithSectionAgentDetails =
      new TdAgentApplicationWithSectionAgentDetails(baseForSectionAgentDetails = afterContactDetailsComplete)

    export tdAgentApplicationLimitedPartnershipWithSectionAgentDetails.sectionAgentDetails

}
