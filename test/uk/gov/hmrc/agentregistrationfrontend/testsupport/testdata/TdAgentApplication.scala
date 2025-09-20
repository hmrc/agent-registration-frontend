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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.AboutYourApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationState

trait TdAgentApplication { dependencies: TdBase =>

  def agentApplicationAfterCreated: AgentApplication = AgentApplication(
    internalUserId = dependencies.internalUserId,
    createdAt = dependencies.instant,
    applicationState = ApplicationState.InProgress,
    utr = Some(dependencies.utr),
    aboutYourApplication = AboutYourApplication(
      businessType = None,
      userRole = None,
      confirmed = false
    ),
    businessDetails = None,
    amlsDetails = None
  )

}
