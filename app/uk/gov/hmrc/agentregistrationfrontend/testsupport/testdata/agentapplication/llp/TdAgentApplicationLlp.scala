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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication.llp

import uk.gov.hmrc.agentregistration.shared.ApplicationState.GrsDataReceived
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.EntityCheckResult
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdGrs

trait TdAgentApplicationLlp { dependencies: (TdBase & TdSectionAmls & TdSectionContactDetails & TdGrs & TdSectionAgentDetails) =>

  object agentApplicationLlp:

    val afterStarted: AgentApplicationLlp = AgentApplicationLlp(
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
      refusalToDealWithCheck = EntityCheckResult.NotChecked,
      companyStatusCheckResult = EntityCheckResult.NotChecked,
      hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet
    )

    val afterGrsDataReceived: AgentApplicationLlp = afterStarted.copy(
      businessDetails = Some(
        dependencies.grs.llp.businessDetails
      ),
      applicationState = GrsDataReceived
    )

    val afterHmrcEntityVerificationPass: AgentApplicationLlp = afterGrsDataReceived.copy(
      refusalToDealWithCheck = EntityCheckResult.Pass
    )

    val afterHmrcEntityVerificationFail: AgentApplicationLlp = afterGrsDataReceived.copy(
      refusalToDealWithCheck = EntityCheckResult.Fail
    )

    val afterCompaniesHouseStatusCheckPass: AgentApplicationLlp = afterHmrcEntityVerificationPass.copy(
      companyStatusCheckResult = EntityCheckResult.Pass
    )

    val afterCompaniesHouseStatusCheckFail: AgentApplicationLlp = afterHmrcEntityVerificationPass.copy(
      companyStatusCheckResult = EntityCheckResult.Fail
    )

    val afterContactDetailsComplete: AgentApplicationLlp = afterCompaniesHouseStatusCheckPass.copy(
      applicantContactDetails = Some(dependencies.applicantContactDetails),
      agentDetails = None
    )

    val afterAgentDetailsComplete: AgentApplicationLlp = afterContactDetailsComplete.copy(
      agentDetails = Some(dependencies.completeAgentDetails)
    )

    val afterAmlsComplete: AgentApplicationLlp = afterAgentDetailsComplete.copy(
      amlsDetails = Some(dependencies.completeAmlsDetails)
    )

    val afterHmrcStandardForAgentsAgreed: AgentApplicationLlp = afterAmlsComplete.copy(
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed
    )

    val afterDeclarationSubmitted: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
      applicationState = ApplicationState.Submitted
    )

    val baseForSectionAmls: AgentApplicationLlp = afterGrsDataReceived
    protected val agentApplicationLlpWithSectionAmls = new AgentApplicationLlpWithSectionAmls(baseForSectionAmls = baseForSectionAmls)
    export agentApplicationLlpWithSectionAmls.sectionAmls

    val baseForSectionContactDetails: AgentApplicationLlp = afterGrsDataReceived
    protected val tdAgentApplicationLlpWithSectionContactDetails =
      new TdAgentApplicationLlpWithSectionContactDetails(baseForSectionContactDetails = baseForSectionContactDetails)

    export tdAgentApplicationLlpWithSectionContactDetails.sectionContactDetails

    protected val tdAgentApplicationLlpWithSectionAgentDetails =
      new TdAgentApplicationLlpWithSectionAgentDetails(baseForSectionAgentDetails = afterContactDetailsComplete)

    export tdAgentApplicationLlpWithSectionAgentDetails.sectionAgentDetails

}
