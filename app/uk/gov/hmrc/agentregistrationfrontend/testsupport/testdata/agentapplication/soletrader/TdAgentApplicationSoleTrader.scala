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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication.soletrader

import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.ApplicationState.GrsDataReceived
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication.llp.TdSectionAgentDetails
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication.llp.TdSectionAmls
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication.llp.TdSectionContactDetails
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdGrs

trait TdAgentApplicationSoleTrader { dependencies: (TdBase & TdSectionAmls & TdSectionContactDetails & TdGrs & TdSectionAgentDetails) =>

  object agentApplicationSoleTrader:

    val afterStarted: AgentApplicationSoleTrader = AgentApplicationSoleTrader(
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
      deceasedCheck = None,
      hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet
    )

    val afterGrsDataReceived: AgentApplicationSoleTrader = afterStarted.copy(
      businessDetails = Some(
        dependencies.grs.soleTrader.businessDetails
      ),
      applicationState = GrsDataReceived
    )

    val afterRefusalToDealWithCheckPass: AgentApplicationSoleTrader = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(EntityCheckResult.Pass)
    )

    val afterRefusalToDealWithCheckFail: AgentApplicationSoleTrader = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(EntityCheckResult.Fail)
    )

    val afterDeceasedCheckPass: AgentApplicationSoleTrader = afterRefusalToDealWithCheckPass.copy(
      deceasedCheck = Some(EntityCheckResult.Pass)
    )

    val afterDeceasedCheckFail: AgentApplicationSoleTrader = afterRefusalToDealWithCheckPass.copy(
      deceasedCheck = Some(EntityCheckResult.Fail)
    )

    val afterContactDetailsComplete: AgentApplicationSoleTrader = afterDeceasedCheckPass.copy(
      applicantContactDetails = Some(dependencies.applicantContactDetails),
      agentDetails = None
    )

    val afterAgentDetailsComplete: AgentApplicationSoleTrader = afterContactDetailsComplete.copy(
      agentDetails = Some(dependencies.completeAgentDetails)
    )

    val afterAmlsComplete: AgentApplicationSoleTrader = afterAgentDetailsComplete.copy(
      amlsDetails = Some(dependencies.completeAmlsDetails)
    )

    val afterHmrcStandardForAgentsAgreed: AgentApplicationSoleTrader = afterAmlsComplete.copy(
      hmrcStandardForAgentsAgreed = StateOfAgreement.Agreed
    )

    val afterDeclarationSubmitted: AgentApplicationSoleTrader = afterHmrcStandardForAgentsAgreed.copy(
      applicationState = ApplicationState.Submitted
    )

//    val baseForSectionAmls: AgentApplicationSoleTrader = afterGrsDataReceived
//    protected val agentApplicationLlpWithSectionAmls = new AgentApplicationLlpWithSectionAmls(baseForSectionAmls = baseForSectionAmls)
//    export agentApplicationLlpWithSectionAmls.sectionAmls

//    val baseForSectionContactDetails: AgentApplicationSoleTrader = afterGrsDataReceived
//    protected val tdAgentApplicationLlpWithSectionContactDetails =
//      new TdAgentApplicationLlpWithSectionContactDetails(baseForSectionContactDetails = baseForSectionContactDetails)

//    export tdAgentApplicationLlpWithSectionContactDetails.sectionContactDetails
//
//    protected val tdAgentApplicationLlpWithSectionAgentDetails =
//      new TdAgentApplicationLlpWithSectionAgentDetails(baseForSectionAgentDetails = afterContactDetailsComplete)
//
//    export tdAgentApplicationLlpWithSectionAgentDetails.sectionAgentDetails

}
