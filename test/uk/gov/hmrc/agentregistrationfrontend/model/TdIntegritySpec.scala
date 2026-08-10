/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.model

import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.dataintegrity.DataIntegrityChecks.dataIntegrityViolations
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll

class TdIntegritySpec
extends UnitSpec:

  private val td = TdAll.tdAll

  "all named journey snapshots" should:
    "satisfy their per-state integrity invariants" in:
      val snapshots: Seq[(String, AgentApplication)] = Seq(
        // LLP
        "llp.afterStarted" -> td.agentApplicationLlp.afterStarted,
        "llp.afterGrsDataReceived" -> td.agentApplicationLlp.afterGrsDataReceived,
        "llp.afterAmlsComplete" -> td.agentApplicationLlp.afterAmlsComplete,
        "llp.afterHmrcStandardForAgentsAgreed" -> td.agentApplicationLlp.afterHmrcStandardForAgentsAgreed,
        "llp.afterDeclarationSubmitted" -> td.agentApplicationLlp.afterDeclarationSubmitted,
        "llp.afterSentToMinerva" -> td.agentApplicationLlp.afterSentToMinerva,
        "llp.afterRiskingCompletedFixable" -> td.agentApplicationLlp.afterRiskingCompletedFixable,
        "llp.afterRiskingCompletedFixableNonHmrcAmls" -> td.agentApplicationLlp.afterRiskingCompletedFixableNonHmrcAmls,
        "llp.afterRiskingCompletedNonFixable" -> td.agentApplicationLlp.afterRiskingCompletedNonFixable,
        "llp.afterRiskingCompletedFixableFixed" -> td.agentApplicationLlp.afterRiskingCompletedFixableFixed,
        "llp.afterResubmitted" -> td.agentApplicationLlp.afterResubmitted,
        // SoleTrader
        "soleTrader.afterStarted" -> td.agentApplicationSoleTrader.afterStarted,
        "soleTrader.afterGrsDataReceived" -> td.agentApplicationSoleTrader.afterGrsDataReceived,
        "soleTrader.afterDeclarationSubmitted" -> td.agentApplicationSoleTrader.afterDeclarationSubmitted,
        // LimitedCompany
        "limitedCompany.afterStarted" -> td.agentApplicationLimitedCompany.afterStarted,
        "limitedCompany.afterGrsDataReceived" -> td.agentApplicationLimitedCompany.afterGrsDataReceived,
        "limitedCompany.afterDeclarationSubmitted" -> td.agentApplicationLimitedCompany.afterDeclarationSubmitted,
        // GeneralPartnership
        "generalPartnership.afterStarted" -> td.agentApplicationGeneralPartnership.afterStarted,
        "generalPartnership.afterGrsDataReceived" -> td.agentApplicationGeneralPartnership.afterGrsDataReceived,
        "generalPartnership.afterDeclarationSubmitted" -> td.agentApplicationGeneralPartnership.afterDeclarationSubmitted,
        // LimitedPartnership
        "limitedPartnership.afterStarted" -> td.agentApplicationLimitedPartnership.afterStarted,
        "limitedPartnership.afterGrsDataReceived" -> td.agentApplicationLimitedPartnership.afterGrsDataReceived,
        "limitedPartnership.afterDeclarationSubmitted" -> td.agentApplicationLimitedPartnership.afterDeclarationSubmitted,
        // ScottishLimitedPartnership
        "scottishLimitedPartnership.afterStarted" -> td.agentApplicationScottishLimitedPartnership.afterStarted,
        "scottishLimitedPartnership.afterGrsDataReceived" -> td.agentApplicationScottishLimitedPartnership.afterGrsDataReceived,
        "scottishLimitedPartnership.afterDeclarationSubmitted" -> td.agentApplicationScottishLimitedPartnership.afterDeclarationSubmitted,
        // ScottishPartnership
        "scottishPartnership.afterStarted" -> td.agentApplicationScottishPartnership.afterStarted,
        "scottishPartnership.afterGrsDataReceived" -> td.agentApplicationScottishPartnership.afterGrsDataReceived,
        "scottishPartnership.afterDeclarationSubmitted" -> td.agentApplicationScottishPartnership.afterDeclarationSubmitted
      )

      val violations: Seq[String] = snapshots.flatMap: (name, app) =>
        dataIntegrityViolations(app).map(v => s"$name -> $v")

      withClue(s"${violations.size} snapshot(s) drifted:\n${violations.mkString("\n")}\n"):
        violations shouldBe empty
