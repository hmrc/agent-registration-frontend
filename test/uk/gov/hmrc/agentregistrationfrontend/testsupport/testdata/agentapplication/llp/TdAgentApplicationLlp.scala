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
import uk.gov.hmrc.agentregistration.shared.BusinessDetailsLlp
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdGrs

trait TdAgentApplicationLlp { dependencies: (TdBase & TdSectionAmls & TdSectionContactDetails & TdGrs) =>

  object agentApplicationLlp:

    val afterStarted: AgentApplicationLlp = AgentApplicationLlp(
      internalUserId = dependencies.internalUserId,
      linkId = dependencies.linkId,
      groupId = dependencies.groupId,
      createdAt = dependencies.nowAsInstant,
      applicationState = ApplicationState.Started,
      businessDetails = None,
      applicantContactDetails = None,
      amlsDetails = None
    )

    val afterGrsDataReceived: AgentApplicationLlp = afterStarted.copy(
      businessDetails = Some(
        dependencies.grs.llp.businessDetails
      ),
      applicationState = GrsDataReceived
    )

    val baseForSectionAmls: AgentApplicationLlp = afterGrsDataReceived
    protected val agentApplicationLlpWithSectionAmls = new AgentApplicationLlpWithSectionAmls(baseForSectionAmls = baseForSectionAmls)
    export agentApplicationLlpWithSectionAmls.sectionAmls

    val baseForSectionContactDetails: AgentApplicationLlp = afterGrsDataReceived
    protected val tdAgentApplicationLlpWithSectionContactDetails =
      new TdAgentApplicationLlpWithSectionContactDetails(baseForSectionContactDetails = baseForSectionContactDetails)

    export tdAgentApplicationLlpWithSectionContactDetails.sectionContactDetails

}
