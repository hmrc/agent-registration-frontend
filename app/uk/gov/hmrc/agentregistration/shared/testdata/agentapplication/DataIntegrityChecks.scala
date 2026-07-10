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

import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistration.shared.*

object DataIntegrityChecks:

  extension [A <: AgentApplication](agentApplication: A)

    def assertDataIntegrity(): A =
      agentApplication.applicationState match
        case ApplicationState.Started => whenStarted()
        case ApplicationState.GrsDataReceived => whenGrsDataReceived()
        case ApplicationState.SentForRisking => whenSentForRisking()
        case ApplicationState.SentToMinerva => whenSentToMinerva()
        case ApplicationState.RiskingCompleted => whenRiskingCompleted()
      agentApplication

    private def check(
      condition: Boolean,
      message: String
    ): Unit = if !condition then throw new IllegalStateException(s"integrity check failed: $message")

    private def businessDetailsDefined: Boolean =
      agentApplication match
        case a: AgentApplicationSoleTrader => a.businessDetails.isDefined
        case a: AgentApplicationLlp => a.businessDetails.isDefined
        case a: AgentApplicationLimitedCompany => a.businessDetails.isDefined
        case a: AgentApplicationGeneralPartnership => a.businessDetails.isDefined
        case a: AgentApplicationLimitedPartnership => a.businessDetails.isDefined
        case a: AgentApplicationScottishLimitedPartnership => a.businessDetails.isDefined
        case a: AgentApplicationScottishPartnership => a.businessDetails.isDefined

    private def whenStarted(): Unit =
      check(agentApplication.applicationExpiresAt.isDefined, "applicationExpiresAt should be defined in Started state")
      check(agentApplication.submittedAt.isEmpty, "submittedAt should not be defined in Started state")
      check(agentApplication.applicantContactDetails.isEmpty, "applicantContactDetails should not be defined in Started state")
      check(agentApplication.amlsDetails.isEmpty, "amlsDetails should not be defined in Started state")
      check(agentApplication.agentDetails.isEmpty, "agentDetails should not be defined in Started state")
      check(agentApplication.refusalToDealWithCheckResult.isEmpty, "refusalToDealWithCheckResult should not be defined in Started state")
      check(agentApplication.globalAsaEnrolmentCheckResult.isEmpty, "globalAsaEnrolmentCheckResult should not be defined in Started state")
      check(agentApplication.hasOtherRelevantIndividuals.isEmpty, "hasOtherRelevantIndividuals should not be defined in Started state")
      check(agentApplication.vrns.isEmpty, "vrns should not be defined in Started state")
      check(agentApplication.payeRefs.isEmpty, "payeRefs should not be defined in Started state")
      check(agentApplication.riskingOutcomeApplication.isEmpty, "riskingOutcomeApplication should not be defined in Started state")
      check(agentApplication.riskingOutcomeEntity.isEmpty, "riskingOutcomeEntity should not be defined in Started state")

    private def whenGrsDataReceived(): Unit = check(businessDetailsDefined, "businessDetails should be defined in GrsDataReceived state")

    private def checkSentForRiskingOrLater(state: String): Unit =
      check(agentApplication.applicationExpiresAt.isEmpty, s"applicationExpiresAt should not be defined in $state state")
      check(agentApplication.submittedAt.isDefined, s"submittedAt should be defined in $state state")
      check(businessDetailsDefined, s"businessDetails should be defined in $state state")
      check(agentApplication.applicantContactDetails.isDefined, s"applicantContactDetails should be defined in $state state")
      check(agentApplication.amlsDetails.isDefined, s"amlsDetails should be defined in $state state")
      check(agentApplication.agentDetails.isDefined, s"agentDetails should be defined in $state state")
      check(agentApplication.refusalToDealWithCheckResult.isDefined, s"refusalToDealWithCheckResult should be defined in $state state")
      check(agentApplication.globalAsaEnrolmentCheckResult.isDefined, s"globalAsaEnrolmentCheckResult should be defined in $state state")
      check(agentApplication.vrns.isDefined, s"vrns should be defined in $state state")
      check(agentApplication.payeRefs.isDefined, s"payeRefs should be defined in $state state")

    private def whenSentForRisking(): Unit = checkSentForRiskingOrLater("SentForRisking")

    private def whenSentToMinerva(): Unit = checkSentForRiskingOrLater("SentToMinerva")

    private def whenRiskingCompleted(): Unit =
      checkSentForRiskingOrLater("RiskingCompleted")
      val roa: RiskingOutcomeApplication = agentApplication.riskingOutcomeApplication.getOrThrowExpectedDataMissing(
        "integrity check failed:riskingOutcomeApplication should be defined in this state"
      )
      val roe: RiskingOutcomeEntity = agentApplication.riskingOutcomeEntity.getOrThrowExpectedDataMissing(
        "integrity check failed:riskingOutcomeEntity should be defined in this state"
      )
      (roa.outcome, roe) match
        case (RiskingOutcomeApplication.Outcome.Approved, RiskingOutcomeEntity.Approved) => ()
        case (RiskingOutcomeApplication.Outcome.Approved, _: RiskingOutcomeEntity.FailedFixable) =>
          throw new IllegalStateException("integrity check failed: riskingOutcomeApplication is approved but riskingOutcomeEntity is not")
        case (RiskingOutcomeApplication.Outcome.Approved, _: RiskingOutcomeEntity.FailedNonFixable) =>
          throw new IllegalStateException("integrity check failed: riskingOutcomeApplication is approved but riskingOutcomeEntity is not")
        case (RiskingOutcomeApplication.Outcome.FailedFixable, RiskingOutcomeEntity.Approved) => ()
        case (RiskingOutcomeApplication.Outcome.FailedFixable, _: RiskingOutcomeEntity.FailedFixable) => ()
        case (RiskingOutcomeApplication.Outcome.FailedFixable, _: RiskingOutcomeEntity.FailedNonFixable) =>
          throw new IllegalStateException("integrity check failed: riskingOutcomeApplication is failed fixable but riskingOutcomeEntity is NotFixable")
        case (_, _) => ()
