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

package uk.gov.hmrc.agentregistrationfrontend.testonly.model

import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.testonly.FindRiskingResultsFilesResponse
import uk.gov.hmrc.agentregistration.shared.testonly.RiskingResultsFileName
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll

import java.time.Instant

class TestRiskingResultsFilenameSpec
extends UnitSpec:

  "combined should name the file after the given instant, to the millisecond, in UTC" in:
    TestRiskingResultsFilename.combined(Instant.parse("2026-10-15T14:46:34.443Z")) shouldBe RiskingResultsFilename("test-only-combined-20261015T144634443Z")

  Seq(
    ("no file holding the application's record", firstRound, noFiles, Nil, false),
    ("a file holding the application's record, in the first round", firstRound, aboutApplication(firstRound, combinedFilename), Nil, true),
    ("the previous round's file, after the resubmission", resubmitted, aboutApplication(resubmitted, previousRoundFilename), Nil, false),
    ("the current round's file, after the resubmission", resubmitted, aboutApplication(resubmitted, currentRoundFilename), Nil, true),
    ("an unprocessed combined file, after the resubmission", resubmitted, aboutApplication(resubmitted, combinedFilename), List(combinedFilename), true),
    ("a processed combined file, after the resubmission", resubmitted, aboutApplication(resubmitted, combinedFilename), Nil, false)
  ).foreach:
    (
      description,
      agentApplication,
      findRiskingResultsFilesResponse,
      unprocessedRiskingResultsFilenames,
      expected
    ) =>
      s"entityResultsSubmitted should be $expected for $description" in:
        TestRiskingResultsFilename.entityResultsSubmitted(
          agentApplication,
          findRiskingResultsFilesResponse,
          unprocessedRiskingResultsFilenames
        ) shouldBe expected

  "individualResultsSubmitted should be true for a file holding the individual's record" in:
    TestRiskingResultsFilename.individualResultsSubmitted(
      individual,
      firstRound,
      aboutIndividual(individual, combinedFilename),
      Nil
    ) shouldBe true

  "individualResultsSubmitted should be false for a file holding only the application's record" in:
    TestRiskingResultsFilename.individualResultsSubmitted(
      individual,
      firstRound,
      aboutApplication(firstRound, combinedFilename),
      Nil
    ) shouldBe false

  private lazy val firstRound: AgentApplication = TdAll.tdAll.agentApplicationLlp.afterSentToMinerva
  private lazy val resubmitted: AgentApplication = TdAll.tdAll.agentApplicationLlp.afterResubmitted
  private lazy val individual: IndividualProvidedDetails = TdAll.tdAll.providedDetails.afterFinished

  private lazy val combinedFilename: RiskingResultsFilename = RiskingResultsFilename("test-only-combined-20261015T110000000Z")
  private lazy val previousRoundFilename: RiskingResultsFilename = RiskingResultsFilename(s"test-only-entity-${resubmitted.applicationReference.value}")
  private lazy val currentRoundFilename: RiskingResultsFilename = TestRiskingResultsFilename.entity(resubmitted)

  private lazy val noFiles: FindRiskingResultsFilesResponse = FindRiskingResultsFilesResponse(
    fileNameByApplicationReference = Map.empty,
    fileNameByPersonReference = Map.empty
  )

  private def aboutApplication(
    agentApplication: AgentApplication,
    riskingResultsFilename: RiskingResultsFilename
  ): FindRiskingResultsFilesResponse = noFiles.copy(
    fileNameByApplicationReference = Map(agentApplication.applicationReference -> RiskingResultsFileName(riskingResultsFilename.value))
  )

  private def aboutIndividual(
    individualProvidedDetails: IndividualProvidedDetails,
    riskingResultsFilename: RiskingResultsFilename
  ): FindRiskingResultsFilesResponse = noFiles.copy(
    fileNameByPersonReference = Map(individualProvidedDetails.personReference -> RiskingResultsFileName(riskingResultsFilename.value))
  )
