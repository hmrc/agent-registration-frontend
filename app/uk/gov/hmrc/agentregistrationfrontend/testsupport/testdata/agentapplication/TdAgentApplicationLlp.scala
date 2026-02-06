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

import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.ApplicationState.GrsDataReceived
import uk.gov.hmrc.agentregistration.shared.CheckResult
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdGrs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication.sections.TdSectionAgentDetails
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication.sections.TdSectionAmls
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication.sections.TdSectionContactDetails

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
      refusalToDealWithCheckResult = None,
      companyStatusCheckResult = None,
      hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
      numberOfRequiredKeyIndividuals = None,
      numberOfOtherRelevantIndividuals = None
    )

    val afterGrsDataReceived: AgentApplicationLlp = afterStarted.copy(
      businessDetails = Some(
        dependencies.grs.llp.businessDetails
      ),
      applicationState = GrsDataReceived
    )

    val afterRefusalToDealWithCheckPass: AgentApplicationLlp = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Pass)
    )

    val afterRefusalToDealWithCheckFail: AgentApplicationLlp = afterGrsDataReceived.copy(
      refusalToDealWithCheckResult = Some(CheckResult.Fail)
    )

    val afterCompaniesHouseStatusCheckPass: AgentApplicationLlp = afterRefusalToDealWithCheckPass.copy(
      companyStatusCheckResult = Some(CheckResult.Pass)
    )

    val afterCompaniesHouseStatusCheckFail: AgentApplicationLlp = afterRefusalToDealWithCheckPass.copy(
      companyStatusCheckResult = Some(CheckResult.Fail)
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

    val afterHowManyKeyIndividuals: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
      numberOfRequiredKeyIndividuals = Some(
        FiveOrLess(
          numberOfKeyIndividuals = 3
        )
      )
    )

    val afterDeclarationSubmitted: AgentApplicationLlp = afterHmrcStandardForAgentsAgreed.copy(
      applicationState = ApplicationState.Submitted
    )

    val baseForSectionAmls: AgentApplicationLlp = afterGrsDataReceived
    protected val agentApplicationLlpWithSectionAmls = new AgentApplicationWithSectionAmls(baseForSectionAmls = baseForSectionAmls)
    export agentApplicationLlpWithSectionAmls.sectionAmls

    val baseForSectionContactDetails: AgentApplicationLlp = afterGrsDataReceived
    protected val tdAgentApplicationLlpWithSectionContactDetails =
      new TdAgentApplicationWithSectionContactDetails(baseForSectionContactDetails = baseForSectionContactDetails)

    export tdAgentApplicationLlpWithSectionContactDetails.sectionContactDetails

    protected val tdAgentApplicationLlpWithSectionAgentDetails =
      new TdAgentApplicationWithSectionAgentDetails(baseForSectionAgentDetails = afterContactDetailsComplete)

    export tdAgentApplicationLlpWithSectionAgentDetails.sectionAgentDetails

}
