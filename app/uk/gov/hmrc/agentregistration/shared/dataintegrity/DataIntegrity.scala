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

package uk.gov.hmrc.agentregistration.shared.dataintegrity

import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.*

/** Runtime data integrity checks for [[AgentApplication]].
  *
  * Deliberately free of any Play or frontend dependency so this file remains valid in every repo which shares this package (agent-registration,
  * agent-registration-frontend, agent-registration-risking). Logging of the violations it returns lives in
  * `uk.gov.hmrc.agentregistrationfrontend.dataintegrity.DataIntegrityLogging`.
  */
object DataIntegrity:

  def violations(agentApplication: AgentApplication): Seq[String] =
    agentApplication.riskingOutcomeApplication match
      case Some(fixable: RiskingOutcomeApplication.FailedFixable) if fixable.reSubmittedAt.isDefined => Resubmission.violations(agentApplication)
      case _ => Submission.violations(agentApplication)

  private inline def violation(
    agentApplication: AgentApplication,
    condition: Boolean,
    message: String
  ): Seq[String] =
    if !condition
    then Seq(s"integrity check failed [applicationReference=${agentApplication.applicationReference.value}]: $message")
    else Seq.empty

  private def preSubmitViolations(agentApplication: AgentApplication)(using state: ApplicationState): Seq[String] =
    violation(
      agentApplication,
      agentApplication.applicationExpiresAt.isDefined,
      s"applicationExpiresAt should be defined in $state state"
    ) ++
      violation(
        agentApplication,
        agentApplication.submittedAt.isEmpty,
        s"submittedAt should not be defined in $state state"
      ) ++
      violation(
        agentApplication,
        agentApplication.riskingOutcomeApplication.isEmpty,
        s"riskingOutcomeApplication should not be defined in $state state"
      ) ++
      violation(
        agentApplication,
        agentApplication.riskingOutcomeEntity.isEmpty,
        s"riskingOutcomeEntity should not be defined in $state state"
      )

  private def postSubmitViolations(agentApplication: AgentApplication)(using state: ApplicationState): Seq[String] =
    violation(
      agentApplication,
      agentApplication.applicationExpiresAt.isEmpty,
      s"applicationExpiresAt should not be defined in $state state"
    ) ++
      violation(
        agentApplication,
        agentApplication.submittedAt.isDefined,
        s"submittedAt should be defined in $state state"
      ) ++
      violation(
        agentApplication,
        businessDetailsDefined(agentApplication),
        s"businessDetails should be defined in $state state"
      ) ++
      violation(
        agentApplication,
        agentApplication.applicantContactDetails.isDefined,
        s"applicantContactDetails should be defined in $state state"
      ) ++
      violation(
        agentApplication,
        agentApplication.applicantContactDetails.forall(_.isComplete),
        s"applicantContactDetails should be complete in $state state"
      ) ++
      violation(
        agentApplication,
        agentApplication.amlsDetails.isDefined,
        s"amlsDetails should be defined in $state state"
      ) ++
      violation(
        agentApplication,
        agentApplication.amlsDetails.forall(_.isComplete),
        s"amlsDetails should be complete in $state state"
      ) ++
      violation(
        agentApplication,
        agentApplication.agentDetails.isDefined,
        s"agentDetails should be defined in $state state"
      ) ++
      violation(
        agentApplication,
        agentApplication.agentDetails.forall(_.isComplete),
        s"agentDetails should be complete in $state state"
      ) ++
      violation(
        agentApplication,
        agentApplication.refusalToDealWithCheckResult.isDefined,
        s"refusalToDealWithCheckResult should be defined in $state state"
      ) ++
      violation(
        agentApplication,
        agentApplication.globalAsaEnrolmentCheckResult.isDefined,
        s"globalAsaEnrolmentCheckResult should be defined in $state state"
      ) ++
      violation(
        agentApplication,
        agentApplication.vrns.isDefined,
        s"vrns should be defined in $state state"
      ) ++
      violation(
        agentApplication,
        agentApplication.payeRefs.isDefined,
        s"payeRefs should be defined in $state state"
      )

  private object Submission:

    def violations(agentApplication: AgentApplication): Seq[String] =
      agentApplication.applicationState match
        case ApplicationState.Started => whenStarted(agentApplication)
        case ApplicationState.GrsDataReceived => whenGrsDataReceived(agentApplication)
        case ApplicationState.SentForRisking | ApplicationState.SentToMinerva => whenAwaitingRiskingOutcome(agentApplication)
        case ApplicationState.RiskingCompleted => whenRiskingCompleted(agentApplication)

    private def whenStarted(agentApplication: AgentApplication): Seq[String] =
      given ApplicationState = ApplicationState.Started
      preSubmitViolations(agentApplication)

    private def whenGrsDataReceived(agentApplication: AgentApplication): Seq[String] =
      given ApplicationState = ApplicationState.GrsDataReceived
      preSubmitViolations(agentApplication) ++
        violation(
          agentApplication,
          businessDetailsDefined(agentApplication),
          "businessDetails should be defined in GrsDataReceived state"
        )

    private def whenAwaitingRiskingOutcome(agentApplication: AgentApplication): Seq[String] =
      given state: ApplicationState = agentApplication.applicationState
      postSubmitViolations(agentApplication) ++
        violation(
          agentApplication,
          agentApplication.riskingOutcomeApplication.isEmpty,
          s"riskingOutcomeApplication should not be defined in $state state during first submission"
        ) ++
        violation(
          agentApplication,
          agentApplication.riskingOutcomeEntity.isEmpty,
          s"riskingOutcomeEntity should not be defined in $state state during first submission"
        )

  private object Resubmission:

    def violations(agentApplication: AgentApplication): Seq[String] =
      agentApplication.applicationState match
        case ApplicationState.SentForRisking | ApplicationState.SentToMinerva => whenAwaitingRiskingOutcome(agentApplication)
        case ApplicationState.RiskingCompleted => whenRiskingCompleted(agentApplication)
        case other =>
          violation(
            agentApplication,
            false,
            s"resubmission-in-flight is only valid in SentForRisking, SentToMinerva or RiskingCompleted, found $other"
          )

    private def whenAwaitingRiskingOutcome(agentApplication: AgentApplication): Seq[String] =
      given state: ApplicationState = agentApplication.applicationState
      // `riskingOutcomeApplication.isDefined` is guaranteed by the top-level dispatch (this object is only entered when roa is Some(FailedFixable) with
      // reSubmittedAt defined), so it is not re-asserted here. `riskingOutcomeEntity` is not guaranteed by the dispatch, hence the check below.
      postSubmitViolations(agentApplication) ++
        violation(
          agentApplication,
          agentApplication.riskingOutcomeEntity.isDefined,
          s"riskingOutcomeEntity should be preserved in $state state during resubmission"
        )

  private def whenRiskingCompleted(agentApplication: AgentApplication): Seq[String] =
    given ApplicationState = ApplicationState.RiskingCompleted
    postSubmitViolations(agentApplication) ++
      violation(
        agentApplication,
        agentApplication.riskingOutcomeApplication.isDefined,
        "riskingOutcomeApplication should be defined in RiskingCompleted state"
      ) ++
      violation(
        agentApplication,
        agentApplication.riskingOutcomeEntity.isDefined,
        "riskingOutcomeEntity should be defined in RiskingCompleted state"
      )

  private def businessDetailsDefined(agentApplication: AgentApplication): Boolean =
    agentApplication match
      case a: AgentApplicationSoleTrader => a.businessDetails.isDefined
      case a: AgentApplicationLlp => a.businessDetails.isDefined
      case a: AgentApplicationLimitedCompany => a.businessDetails.isDefined
      case a: AgentApplicationGeneralPartnership => a.businessDetails.isDefined
      case a: AgentApplicationLimitedPartnership => a.businessDetails.isDefined
      case a: AgentApplicationScottishLimitedPartnership => a.businessDetails.isDefined
      case a: AgentApplicationScottishPartnership => a.businessDetails.isDefined
