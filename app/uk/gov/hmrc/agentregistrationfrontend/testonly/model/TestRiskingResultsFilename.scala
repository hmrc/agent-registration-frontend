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
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.shared.testonly.FindRiskingResultsFilesResponse
import uk.gov.hmrc.agentregistration.shared.testonly.RiskingResultsFileName
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Deterministic risking-results filenames for the Minerva simulator's test-only SDES stub, which treats every filename as write-once.
  *
  * With no resubmission, an entity/individual gets exactly one filename for the lifetime of the application — matching the original single-decision flow. Once
  * the application is resubmitted after a FailedFixable risking outcome (`RiskingOutcomeApplication.FailedFixable.reSubmittedAt`), a *new* filename is needed
  * so a second results file can be uploaded — but only for the entity/individual(s) that were actually FailedFixable last time; anything already Approved keeps
  * pointing at its original (already-submitted) filename, since there's nothing new to send for it.
  */
object TestRiskingResultsFilename:

  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmssSSS'Z'").withZone(ZoneOffset.UTC)

  /** Filename for a synthetic test-only results file that combines the records of selected results files into one larger file. */
  def combined(now: Instant): RiskingResultsFilename = RiskingResultsFilename(s"test-only-combined-${formatter.format(now)}")

  /** The filename the entity's current round of risking results is/would be submitted under, derived entirely from the application's own persisted state: the
    * canonical (no-suffix) name unless the entity is currently FailedFixable and a real resubmission (`RiskingOutcomeApplication.FailedFixable.reSubmittedAt`)
    * has happened, in which case that resubmission's own timestamp is used.
    */
  def entity(agentApplication: AgentApplication): RiskingResultsFilename =
    val reSubmittedAt = entityRoundStartedAt(agentApplication)
    RiskingResultsFilename(s"test-only-entity-${agentApplication.applicationReference.value}${suffix(reSubmittedAt)}")

  /** As [[entity]], but for one of the application's individuals. */
  def individual(
    individualProvidedDetails: IndividualProvidedDetails,
    agentApplication: AgentApplication
  ): RiskingResultsFilename =
    val reSubmittedAt = individualRoundStartedAt(individualProvidedDetails, agentApplication)
    RiskingResultsFilename(s"test-only-individual-${individualProvidedDetails.personReference.value}${suffix(reSubmittedAt)}")

  /** The risking results file holding the entity's record, whatever the file is called. */
  def entityResultsFilename(
    agentApplication: AgentApplication,
    findRiskingResultsFilesResponse: FindRiskingResultsFilesResponse
  ): Option[RiskingResultsFilename] = findRiskingResultsFilesResponse
    .fileNameByApplicationReference
    .get(agentApplication.applicationReference)
    .map(toRiskingResultsFilename)

  /** As [[entityResultsFilename]], but for one of the application's individuals. */
  def individualResultsFilename(
    individualProvidedDetails: IndividualProvidedDetails,
    findRiskingResultsFilesResponse: FindRiskingResultsFilesResponse
  ): Option[RiskingResultsFilename] = findRiskingResultsFilesResponse
    .fileNameByPersonReference
    .get(individualProvidedDetails.personReference)
    .map(toRiskingResultsFilename)

  /** Whether the entity's results for its current round are in the file holding its record. */
  def entityResultsSubmitted(
    agentApplication: AgentApplication,
    findRiskingResultsFilesResponse: FindRiskingResultsFilesResponse,
    unprocessedRiskingResultsFilenames: List[RiskingResultsFilename]
  ): Boolean = entityResultsFilename(agentApplication, findRiskingResultsFilesResponse).exists((riskingResultsFilename: RiskingResultsFilename) =>
    inCurrentRound(
      riskingResultsFilename = riskingResultsFilename,
      roundStartedAt = entityRoundStartedAt(agentApplication),
      currentRoundFilename = entity(agentApplication),
      unprocessedRiskingResultsFilenames = unprocessedRiskingResultsFilenames
    )
  )

  /** As [[entityResultsSubmitted]], but for one of the application's individuals. */
  def individualResultsSubmitted(
    individualProvidedDetails: IndividualProvidedDetails,
    agentApplication: AgentApplication,
    findRiskingResultsFilesResponse: FindRiskingResultsFilesResponse,
    unprocessedRiskingResultsFilenames: List[RiskingResultsFilename]
  ): Boolean = individualResultsFilename(individualProvidedDetails, findRiskingResultsFilesResponse).exists((riskingResultsFilename: RiskingResultsFilename) =>
    inCurrentRound(
      riskingResultsFilename = riskingResultsFilename,
      roundStartedAt = individualRoundStartedAt(individualProvidedDetails, agentApplication),
      currentRoundFilename = individual(individualProvidedDetails, agentApplication),
      unprocessedRiskingResultsFilenames = unprocessedRiskingResultsFilenames
    )
  )

  /** Every file belongs to the first round. The previous round's file has been processed and is named differently, so after a resubmission a file belongs to
    * the current round if it has the current round's name or is not processed yet.
    */
  private def inCurrentRound(
    riskingResultsFilename: RiskingResultsFilename,
    roundStartedAt: Option[Instant],
    currentRoundFilename: RiskingResultsFilename,
    unprocessedRiskingResultsFilenames: List[RiskingResultsFilename]
  ): Boolean =
    roundStartedAt.isEmpty
      || riskingResultsFilename === currentRoundFilename
      || unprocessedRiskingResultsFilenames.contains(riskingResultsFilename)

  private def toRiskingResultsFilename(riskingResultsFileName: RiskingResultsFileName): RiskingResultsFilename = RiskingResultsFilename(
    riskingResultsFileName.value
  )

  private def entityRoundStartedAt(agentApplication: AgentApplication): Option[Instant] = currentReSubmittedAtFor(
    agentApplication.riskingOutcomeEntity.exists(isFailedFixable),
    agentApplication
  )

  private def individualRoundStartedAt(
    individualProvidedDetails: IndividualProvidedDetails,
    agentApplication: AgentApplication
  ): Option[Instant] = currentReSubmittedAtFor(
    individualProvidedDetails.riskingOutcomeIndividual.exists(isFailedFixable),
    agentApplication
  )

  private def isFailedFixable(outcome: RiskingOutcomeEntity): Boolean =
    outcome match
      case _: RiskingOutcomeEntity.FailedFixable => true
      case _ => false

  private def isFailedFixable(outcome: RiskingOutcomeIndividual): Boolean =
    outcome match
      case _: RiskingOutcomeIndividual.FailedFixable => true
      case _ => false

  private def currentReSubmittedAtFor(
    wasFailedFixable: Boolean,
    agentApplication: AgentApplication
  ): Option[Instant] =
    if wasFailedFixable then
      agentApplication.riskingOutcomeApplication.collect { case f: RiskingOutcomeApplication.FailedFixable => f.reSubmittedAt }.flatten
    else
      None

  private def suffix(reSubmittedAt: Option[Instant]): String = reSubmittedAt.map(instant => s"-${formatter.format(instant)}").getOrElse("")
