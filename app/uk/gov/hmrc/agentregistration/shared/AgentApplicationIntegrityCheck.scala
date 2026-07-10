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

package uk.gov.hmrc.agentregistration.shared

import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing

object AgentApplicationIntegrityCheck:

  extension (agentApplication: AgentApplication)

    def doDataIntegrityChecks(): Unit =
      agentApplication.applicationState match
        case ApplicationState.Started => whenStarted()
        case ApplicationState.GrsDataReceived => whenGrsDataReceived()
        case ApplicationState.SentForRisking => whenSentForRisking()
        case ApplicationState.SentToMinerva => whenSentToMinerva()
        case ApplicationState.RiskingCompleted => whenRiskingCompleted()

    private def whenStarted(): Unit = ()
    private def whenGrsDataReceived(): Unit = ()
    private def whenSentForRisking(): Unit = ()
    private def whenSentToMinerva(): Unit = ()
    private def whenRiskingCompleted(): Unit =
      val roa: RiskingOutcomeApplication = agentApplication.riskingOutcomeApplication.getOrThrowExpectedDataMissing(
        "integrity check failed:riskingOutcomeApplication should be defined in this state"
      )
      val roe: RiskingOutcomeEntity = agentApplication.riskingOutcomeEntity.getOrThrowExpectedDataMissing(
        "integrity check failed:riskingOutcomeEntity should be defined in this state"
      )
      (roa.outcome, roe) match
        case (RiskingOutcomeApplication.Outcome.Approved, RiskingOutcomeEntity.Approved) => ()
        case (RiskingOutcomeApplication.Outcome.Approved, _) =>
          throw new IllegalStateException("integrity check failed: riskingOutcomeApplication is approved but riskingOutcomeEntity is not")
        case (RiskingOutcomeApplication.Outcome.FailedFixable, RiskingOutcomeEntity.Approved) => ()
        case (RiskingOutcomeApplication.Outcome.FailedFixable, _: RiskingOutcomeEntity.FailedFixable) => ()
        case (RiskingOutcomeApplication.Outcome.FailedFixable, _: RiskingOutcomeEntity.FailedNonFixable) =>
          throw new IllegalStateException("integrity check failed: riskingOutcomeApplication is failed fixable but riskingOutcomeEntity is NotFixable")
        case (_, _) => ()
