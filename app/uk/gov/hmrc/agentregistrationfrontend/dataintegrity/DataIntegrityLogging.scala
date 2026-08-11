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

package uk.gov.hmrc.agentregistrationfrontend.dataintegrity

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.dataintegrity.DataIntegrity
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogger

/** Logs the violations reported by [[DataIntegrity]].
  *
  * Kept out of the `shared` package on purpose: `shared` is copied verbatim into agent-registration and agent-registration-risking by
  * syncSharedFiles.sh, and this depends on frontend-only types (`RequestAwareLogger`) which do not exist there.
  */
object DataIntegrityLogging:

  extension [A <: AgentApplication](agentApplication: A)

    def logViolations(using
      logger: RequestAwareLogger,
      request: RequestHeader
    ): A =
      DataIntegrity.violations(agentApplication).foreach(msg => logger.error(msg))
      agentApplication
