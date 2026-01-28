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

package uk.gov.hmrc.agentregistrationfrontend.forms

import play.api.data.Form
import play.api.data.Forms
import play.api.data.Forms.mapping
import uk.gov.hmrc.agentregistration.shared.lists.FromFiveOrFewer
import uk.gov.hmrc.agentregistration.shared.lists.FromSixOrMore
import uk.gov.hmrc.agentregistration.shared.lists.RequiredKeyIndividuals
import uk.gov.hmrc.agentregistrationfrontend.forms.formatters.TextFormatter
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.ErrorKeys
import uk.gov.hmrc.agentregistrationfrontend.forms.mappings.Mappings.numberFromString
import uk.gov.voa.play.form.ConditionalMappings.isEqual
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIf

object NumberOfKeyIndividualsForm:

  val key: String = "numberOfKeyIndividuals"
  val exactNumberOfOfficialsKey: String = "exactNumberOfOfficials"
  val numberOfOfficialsWhoDealWithTaxKey: String = "numberOfOfficialsWhoDealWithTax"

  private val intToFromFiveOrFewer: Int => RequiredKeyIndividuals =
    n =>
      FromFiveOrFewer(
        numberToProvideDetails = n
      )

  private val intToFromSixOrMore: Int => RequiredKeyIndividuals =
    n =>
      FromSixOrMore(
        numberToProvideDetails = n
      )

  private val tupleToRequiredKeyIndividuals: (
    String,
    Option[Int],
    Option[Int]
  ) => RequiredKeyIndividuals =
    (
      selected,
      fiveOpt,
      sixOpt
    ) =>
      selected match
        case "FromFiveOrFewer" => intToFromFiveOrFewer(fiveOpt.getOrElse(0))
        case "FromSixOrMore" => intToFromSixOrMore(sixOpt.getOrElse(0))

  private val requiredKeyIndividualsToTuple: RequiredKeyIndividuals => Option[(String, Option[Int], Option[Int])] = {
    case f @ FromFiveOrFewer(n) =>
      Some(
        FromFiveOrFewer.toString,
        Some(n),
        None
      )
    case s @ FromSixOrMore(n) =>
      Some(
        FromSixOrMore.toString,
        None,
        Some(n)
      )
  }

  val form: Form[RequiredKeyIndividuals] = Form(
    mapping =
      mapping(
        key -> Forms.of(TextFormatter(ErrorKeys.requiredFieldErrorMessage(key)))
          .verifying(
            ErrorKeys.requiredFieldErrorMessage(key),
            _.nonEmpty
          ),
        exactNumberOfOfficialsKey -> mandatoryIf(
          isEqual(key, "FromFiveOrFewer"),
          numberFromString(exactNumberOfOfficialsKey)
            .verifying(
              ErrorKeys.invalidInputErrorMessage(exactNumberOfOfficialsKey),
              n => n >= 1 && n <= 5
            )
        ),
        numberOfOfficialsWhoDealWithTaxKey -> mandatoryIf(
          isEqual(key, "FromSixOrMore"),
          numberFromString(numberOfOfficialsWhoDealWithTaxKey)
            .verifying(
              ErrorKeys.invalidInputErrorMessage(numberOfOfficialsWhoDealWithTaxKey),
              n => n >= 1 && n <= 30
            )
        )
      )(tupleToRequiredKeyIndividuals)(requiredKeyIndividualsToTuple)
  )
