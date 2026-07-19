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

/** Flat, structured catalogue of every possible entity (application) risking failure, mirroring
  * `uk.gov.hmrc.agentregistration.shared.risking.EntityFailure` but carrying `checkId`/`reasonCode`/descriptions as real
  * fields rather than scaladoc, so test-only pages can build the `Failure` JSON objects expected by a risking results
  * file (see `agent-registration-risking`'s `RiskingResultRecordSpec` for the JSON shape: reasonCode, reasonDescription,
  * checkId, checkDescription).
  */
enum EntityRiskingFailure(
  val checkId: String,
  val reasonCode: String,
  val checkDescription: String,
  val reasonDescription: String,
  val fixable: Boolean
):

  // Check 3: AMLS
  case Check_3_1
  extends EntityRiskingFailure(
    checkId = "3",
    reasonCode = "3.1",
    checkDescription = "AMLS",
    reasonDescription = "Entity claims AMLS with HMRC but their registration number cannot be found in HMRC's AMLS register",
    fixable = true
  )

  case Check_3_2
  extends EntityRiskingFailure(
    checkId = "3",
    reasonCode = "3.2",
    checkDescription = "AMLS",
    reasonDescription = "Entity claims AMLS with a professional body but their registration number cannot be found in that professional body's AMLS register",
    fixable = true
  )

  case Check_3_3
  extends EntityRiskingFailure(
    checkId = "3",
    reasonCode = "3.3",
    checkDescription = "AMLS",
    reasonDescription = "No proof or evidence of AMLS coverage (file upload)",
    fixable = true
  )

  case Check_3_4
  extends EntityRiskingFailure(
    checkId = "3",
    reasonCode = "3.4",
    checkDescription = "AMLS",
    reasonDescription = "Professional body not on approved list",
    fixable = true
  )

  case Check_3_5
  extends EntityRiskingFailure(
    checkId = "3",
    reasonCode = "3.5",
    checkDescription = "AMLS",
    reasonDescription = "Student membership",
    fixable = true
  )

  // Check 4: Overdue returns
  case Check_4_1
  extends EntityRiskingFailure(
    checkId = "4",
    reasonCode = "4.1",
    checkDescription = "Overdue returns",
    reasonDescription = "One or more overdue SA returns",
    fixable = true
  )

  case Check_4_2
  extends EntityRiskingFailure(
    checkId = "4",
    reasonCode = "4.2",
    checkDescription = "Overdue returns",
    reasonDescription = "One or more overdue CoTax returns",
    fixable = true
  )

  case Check_4_3
  extends EntityRiskingFailure(
    checkId = "4",
    reasonCode = "4.3",
    checkDescription = "Overdue returns",
    reasonDescription = "One or more overdue VAT returns",
    fixable = true
  )

  case Check_4_4
  extends EntityRiskingFailure(
    checkId = "4",
    reasonCode = "4.4",
    checkDescription = "Overdue returns",
    reasonDescription = "One or more overdue PAYE returns",
    fixable = true
  )

  // Check 5: Overdue liabilities
  case Check_5_1
  extends EntityRiskingFailure(
    checkId = "5",
    reasonCode = "5.1",
    checkDescription = "Overdue liabilities",
    reasonDescription = "One or more overdue SA liabilities",
    fixable = true
  )

  case Check_5_2
  extends EntityRiskingFailure(
    checkId = "5",
    reasonCode = "5.2",
    checkDescription = "Overdue liabilities",
    reasonDescription = "One or more overdue CoTax liabilities",
    fixable = true
  )

  case Check_5_3
  extends EntityRiskingFailure(
    checkId = "5",
    reasonCode = "5.3",
    checkDescription = "Overdue liabilities",
    reasonDescription = "One or more overdue VAT liabilities",
    fixable = true
  )

  case Check_5_4
  extends EntityRiskingFailure(
    checkId = "5",
    reasonCode = "5.4",
    checkDescription = "Overdue liabilities",
    reasonDescription = "One or more overdue PAYE liabilities",
    fixable = true
  )

  case Check_5_5
  extends EntityRiskingFailure(
    checkId = "5",
    reasonCode = "5.5",
    checkDescription = "Overdue liabilities",
    reasonDescription = "One or more overdue civil penalties",
    fixable = true
  )

  case Check_5_6
  extends EntityRiskingFailure(
    checkId = "5",
    reasonCode = "5.6",
    checkDescription = "Overdue liabilities",
    reasonDescription = "One or more overdue Stamp Duty liabilities",
    fixable = true
  )

  case Check_5_7
  extends EntityRiskingFailure(
    checkId = "5",
    reasonCode = "5.7",
    checkDescription = "Overdue liabilities",
    reasonDescription = "One or more overdue Capital Gains Tax liabilities",
    fixable = true
  )

  // Check 7: Insolvent (standalone, no sub-reasons)
  case Check_7
  extends EntityRiskingFailure(
    checkId = "7",
    reasonCode = "7",
    checkDescription = "Insolvent",
    reasonDescription = "Insolvent",
    fixable = false
  )

  // Check 8: Anti-avoidance measures or penalties
  case Check_8_1
  extends EntityRiskingFailure(
    checkId = "8",
    reasonCode = "8.1",
    checkDescription = "Anti-avoidance measures or penalties",
    reasonDescription = "Measure - Published Tax Avoidance promoters, enablers and suppliers",
    fixable = false
  )

  case Check_8_4
  extends EntityRiskingFailure(
    checkId = "8",
    reasonCode = "8.4",
    checkDescription = "Anti-avoidance measures or penalties",
    reasonDescription = "POTAS penalty - within 12 months",
    fixable = false
  )

  case Check_8_5
  extends EntityRiskingFailure(
    checkId = "8",
    reasonCode = "8.5",
    checkDescription = "Anti-avoidance measures or penalties",
    reasonDescription = "POTAS penalty - more than 12 months, not paid",
    fixable = true
  )

  case Check_8_6
  extends EntityRiskingFailure(
    checkId = "8",
    reasonCode = "8.6",
    checkDescription = "Anti-avoidance measures or penalties",
    reasonDescription = "Enablers Penalty - within 12 months",
    fixable = false
  )

  case Check_8_7
  extends EntityRiskingFailure(
    checkId = "8",
    reasonCode = "8.7",
    checkDescription = "Anti-avoidance measures or penalties",
    reasonDescription = "Enablers Penalty - more than 12 months, not paid",
    fixable = true
  )
