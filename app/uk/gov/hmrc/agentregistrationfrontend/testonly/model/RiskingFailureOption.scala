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

/** A single failure a test-only user can pick when simulating a risking results file.
  * checkId/reasonCode pairs must match exactly what agent-registration-risking's FailureParser accepts.
  */
final case class RiskingFailureOption(
  checkId: String,
  reasonCode: String,
  checkDescription: String,
  reasonDescription: String
):
  def code: String = s"$checkId:$reasonCode"

object RiskingFailureOption:

  val entityFailureOptions: List[RiskingFailureOption] = List(
    RiskingFailureOption("3", "3.1", "AMLS", "Entity claims AMLS with HMRC but their registration number cannot be found in HMRC's AMLS register"),
    RiskingFailureOption("3", "3.2", "AMLS", "Entity claims AMLS with a professional body but their registration number cannot be found in that professional body's AMLS register"),
    RiskingFailureOption("3", "3.3", "AMLS", "No proof or evidence of AMLS coverage (file upload)"),
    RiskingFailureOption("3", "3.4", "AMLS", "Professional body not on approved list"),
    RiskingFailureOption("3", "3.5", "AMLS", "Student membership"),
    RiskingFailureOption("4", "4.1", "Overdue returns", "One or more overdue SA returns"),
    RiskingFailureOption("4", "4.2", "Overdue returns", "One or more overdue CoTax returns"),
    RiskingFailureOption("4", "4.3", "Overdue returns", "One or more overdue VAT returns"),
    RiskingFailureOption("4", "4.4", "Overdue returns", "One or more overdue PAYE returns"),
    RiskingFailureOption("5", "5.1", "Overdue liabilities", "One or more overdue SA liabilities"),
    RiskingFailureOption("5", "5.2", "Overdue liabilities", "One or more overdue CoTax liabilities"),
    RiskingFailureOption("5", "5.3", "Overdue liabilities", "One or more overdue VAT liabilities"),
    RiskingFailureOption("5", "5.4", "Overdue liabilities", "One or more overdue PAYE liabilities"),
    RiskingFailureOption("5", "5.5", "Overdue liabilities", "One or more overdue civil penalties"),
    RiskingFailureOption("5", "5.6", "Overdue liabilities", "One or more overdue Stamp Duty liabilities"),
    RiskingFailureOption("5", "5.7", "Overdue liabilities", "One or more overdue Capital Gains Tax liabilities"),
    RiskingFailureOption("7", "7", "Insolvency", "Insolvent"),
    RiskingFailureOption("8", "8.1", "Anti-avoidance measures or penalties", "Published Tax Avoidance promoters, enablers and suppliers"),
    RiskingFailureOption("8", "8.4", "Anti-avoidance measures or penalties", "POTAS penalty - within 12 months"),
    RiskingFailureOption("8", "8.5", "Anti-avoidance measures or penalties", "POTAS penalty - more than 12 months, not paid"),
    RiskingFailureOption("8", "8.6", "Anti-avoidance measures or penalties", "Enablers Penalty - within 12 months"),
    RiskingFailureOption("8", "8.7", "Anti-avoidance measures or penalties", "Enablers Penalty - more than 12 months, not paid")
  )

  val individualFailureOptions: List[RiskingFailureOption] = List(
    RiskingFailureOption("4", "4.1", "Overdue returns", "One or more overdue SA returns"),
    RiskingFailureOption("4", "4.3", "Overdue returns", "One or more overdue VAT returns"),
    RiskingFailureOption("4", "4.4", "Overdue returns", "One or more overdue PAYE returns"),
    RiskingFailureOption("5", "5.1", "Overdue liabilities", "One or more overdue SA liabilities"),
    RiskingFailureOption("5", "5.3", "Overdue liabilities", "One or more overdue VAT liabilities"),
    RiskingFailureOption("5", "5.4", "Overdue liabilities", "One or more overdue PAYE liabilities"),
    RiskingFailureOption("5", "5.5", "Overdue liabilities", "One or more overdue civil penalties"),
    RiskingFailureOption("5", "5.6", "Overdue liabilities", "One or more overdue Stamp Duty liabilities"),
    RiskingFailureOption("5", "5.7", "Overdue liabilities", "One or more overdue Capital Gains Tax liabilities"),
    RiskingFailureOption("6", "6", "Companies House", "Disqualified as a director on Companies House"),
    RiskingFailureOption("7", "7", "Insolvency", "Insolvent"),
    RiskingFailureOption("8", "8.1", "Anti-avoidance measures or penalties", "Published Tax Avoidance promoters, enablers and suppliers"),
    RiskingFailureOption("8", "8.6", "Anti-avoidance measures or penalties", "Enablers Penalty - within 12 months"),
    RiskingFailureOption("8", "8.7", "Anti-avoidance measures or penalties", "Enablers Penalty - more than 12 months, not paid"),
    RiskingFailureOption("9", "9", "Criminal convictions", "Relevant criminal convictions"),
    RiskingFailureOption("10", "10.1", "Identity verification", "Unable to match Name and DOB against references"),
    RiskingFailureOption("10", "10.2", "Identity verification", "Unable to match Name and DOB against references and Missing SA-UTR")
  )

  private val byCode: Map[String, RiskingFailureOption] =
    (entityFailureOptions ++ individualFailureOptions).map(option => option.code -> option).toMap

  def find(code: String): Option[RiskingFailureOption] = byCode.get(code)
