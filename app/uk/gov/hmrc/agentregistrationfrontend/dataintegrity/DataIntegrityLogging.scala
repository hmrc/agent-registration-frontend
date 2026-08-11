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

import scala.util.Failure
import scala.util.Success
import scala.util.Try

object DataIntegrityLogging:

  extension (agentApplication: AgentApplication)

    // Never throws: integrity checking is diagnostic and must not be able to fail a user's journey.
    def logViolations(using
      logger: RequestAwareLogger,
      request: RequestHeader
    ): Unit =
      Try(DataIntegrity.violations(agentApplication)) match
        case Success(violations) => violations.foreach(violation => logger.error(violation))
        case Failure(exception) =>
          logger.error(
            s"data integrity check could not be run [applicationReference=${agentApplication.applicationReference.value}]",
            exception
          )
