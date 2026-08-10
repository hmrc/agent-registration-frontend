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

package uk.gov.hmrc.agentregistrationfrontend.model

import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.dataintegrity.DataIntegrityChecks.dataIntegrityViolations
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll

class DataIntegrityChecksSpec
extends UnitSpec:

  private val td = TdAll.tdAll
  private val llp = td.agentApplicationLlp

  "a valid Started snapshot" should:
    "produce no violations" in:
      dataIntegrityViolations(llp.afterStarted) shouldBe empty

  "an inconsistent Started app" should:
    "flag missing applicationExpiresAt" in:
      val bad = llp.afterStarted.copy(applicationExpiresAt = None)
      dataIntegrityViolations(bad).exists(_.contains("applicationExpiresAt should be defined in Started state")) shouldBe true

    "flag submittedAt set" in:
      val bad = llp.afterStarted.copy(submittedAt = Some(td.nowAsInstant))
      dataIntegrityViolations(bad).exists(_.contains("submittedAt should not be defined in Started state")) shouldBe true

    "flag riskingOutcomeApplication set" in:
      val bad = llp.afterStarted.copy(riskingOutcomeApplication = Some(td.riskingOutcomeApplication.failedFixable))
      dataIntegrityViolations(bad).exists(_.contains("riskingOutcomeApplication should not be defined in Started state")) shouldBe true

    "include applicationReference in the message" in:
      val bad = llp.afterStarted.copy(submittedAt = Some(td.nowAsInstant))
      val violation = dataIntegrityViolations(bad).headOption.getOrElse(fail("expected at least one violation"))
      violation should include(s"applicationReference=${bad.applicationReference.value}")

    "not flag user-populated details before GRS returns (section controllers allow pre-GRS fills)" in:
      val bad = llp.afterStarted.copy(
        applicantContactDetails = Some(td.applicantContactDetails),
        amlsDetails = Some(td.completeAmlsDetails),
        agentDetails = Some(td.completeAgentDetails)
      )
      dataIntegrityViolations(bad) shouldBe empty

  "a valid GrsDataReceived snapshot" should:
    "produce no violations" in:
      dataIntegrityViolations(llp.afterGrsDataReceived) shouldBe empty

  "an inconsistent GrsDataReceived app" should:
    "flag missing businessDetails" in:
      val bad = llp.afterGrsDataReceived.copy(businessDetails = None)
      dataIntegrityViolations(bad).exists(_.contains("businessDetails should be defined in GrsDataReceived state")) shouldBe true

    "flag submittedAt set" in:
      val bad = llp.afterGrsDataReceived.copy(submittedAt = Some(td.nowAsInstant))
      dataIntegrityViolations(bad).exists(_.contains("submittedAt should not be defined in GrsDataReceived state")) shouldBe true

    "flag riskingOutcomeEntity set" in:
      val bad = llp.afterGrsDataReceived.copy(riskingOutcomeEntity = Some(td.riskingOutcomeEntityFailedFixable()))
      dataIntegrityViolations(bad).exists(_.contains("riskingOutcomeEntity should not be defined in GrsDataReceived state")) shouldBe true

  "a valid first-submission SentForRisking snapshot" should:
    "produce no violations" in:
      dataIntegrityViolations(llp.afterDeclarationSubmitted) shouldBe empty

  "an inconsistent first-submission SentForRisking app" should:
    "flag missing amlsDetails" in:
      val bad = llp.afterDeclarationSubmitted.copy(amlsDetails = None)
      dataIntegrityViolations(bad).exists(_.contains("amlsDetails should be defined in SentForRisking state")) shouldBe true

    "flag riskingOutcomeApplication defined during first submission" in:
      val bad = llp.afterDeclarationSubmitted.copy(riskingOutcomeApplication = Some(td.riskingOutcomeApplication.failedFixable))
      dataIntegrityViolations(bad).exists(
        _.contains("riskingOutcomeApplication should not be defined in SentForRisking state during first submission")
      ) shouldBe true

  "a valid SentToMinerva snapshot" should:
    "produce no violations" in:
      dataIntegrityViolations(llp.afterSentToMinerva) shouldBe empty

  "a valid RiskingCompleted snapshot" should:
    "produce no violations for FailedFixable + FailedFixable" in:
      dataIntegrityViolations(llp.afterRiskingCompletedFixable) shouldBe empty

    "produce no violations for FailedFixable + Approved (fixable individuals)" in:
      dataIntegrityViolations(llp.afterRiskingCompletedApprovedWithFixableIndividuals) shouldBe empty

  "an inconsistent RiskingCompleted app" should:
    "flag missing riskingOutcomeApplication" in:
      val bad = llp.afterRiskingCompletedFixable.copy(riskingOutcomeApplication = None)
      dataIntegrityViolations(bad).exists(_.contains("riskingOutcomeApplication should be defined in RiskingCompleted state")) shouldBe true

  "a valid resubmission-in-flight snapshot" should:
    "produce no violations when in SentForRisking with reSubmittedAt preserved" in:
      dataIntegrityViolations(llp.afterResubmitted) shouldBe empty

  "an inconsistent resubmission-in-flight app" should:
    "flag Started state as invalid during resubmission" in:
      val bad = llp.afterResubmitted.copy(applicationState = ApplicationState.Started)
      dataIntegrityViolations(bad).exists(_.contains("resubmission-in-flight is only valid in SentForRisking, SentToMinerva or RiskingCompleted")) shouldBe true
