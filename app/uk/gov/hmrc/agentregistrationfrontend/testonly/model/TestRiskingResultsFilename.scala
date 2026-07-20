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

import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Deterministic risking-results filenames for the Minerva simulator's test-only SDES stub, which treats every filename as write-once.
  *
  * With no resubmission, an entity/individual gets exactly one filename for the lifetime of the application — matching the original single-decision flow. Once
  * the application is resubmitted after a FailedFixable risking outcome (`RiskingOutcomeApplication.FailedFixable.reSubmittedAt`), a *new* filename is needed
  * so a second results file can be uploaded — but only for the entity/individual(s) that were actually FailedFixable last time; anything already Approved keeps
  * pointing at its original (already-submitted) filename, since there's nothing new to send for it.
  *
  * `reSubmittedAt` is used as the filename suffix itself rather than "now" at submit time, so the expected filename for the current cycle can be computed
  * identically wherever it's needed (view rendering, submission) without listing and parsing existing filenames to find "the latest".
  */
object TestRiskingResultsFilename:

  private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmssSSS'Z'").withZone(ZoneOffset.UTC)

  def entity(
    applicationReference: ApplicationReference,
    reSubmittedAt: Option[Instant]
  ): String = s"test-only-entity-${applicationReference.value}${suffix(reSubmittedAt)}"

  def individual(
    personReference: PersonReference,
    reSubmittedAt: Option[Instant]
  ): String = s"test-only-individual-${personReference.value}${suffix(reSubmittedAt)}"

  private def suffix(reSubmittedAt: Option[Instant]): String = reSubmittedAt.map(instant => s"-${formatter.format(instant)}").getOrElse("")
