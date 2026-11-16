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

package uk.gov.hmrc.agentregistrationfrontend.forms.applicant.listdetails.nonincorporated

import play.api.data.Form
import play.api.data.Forms
import play.api.data.Forms.mapping
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfPartners
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistrationfrontend.forms.formatters.FormatterFactory
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.ErrorKeys
import uk.gov.hmrc.agentregistrationfrontend.forms.mappings.Mappings.numberFromString
import uk.gov.voa.play.form.ConditionalMappings.isEqual
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIf

object NumberOfPartnersForm:

  val whichNumberType: String = "numberOfKeyIndividuals"
  val exactNumberOfPartners: String = "exactNumberOfOfficials"

  enum HowManyIndividualsOption:

    case FiveOrLess
    case SixOrMore

  val form: Form[NumberOfPartners] = Form(
    mapping = (mapping(
      whichNumberType -> Forms.of(using
        FormatterFactory.makeEnumFormatter[HowManyIndividualsOption](
          errorMessageIfMissing = ErrorKeys.requiredFieldErrorMessage(whichNumberType)
        )
      ),
      exactNumberOfPartners -> mandatoryIf(
        isEqual(whichNumberType, HowManyIndividualsOption.FiveOrLess.toString),
        numberFromString(exactNumberOfPartners)
          .transform[FiveOrLess](FiveOrLess.apply, _.numberOfKeyIndividuals)
          .verifying(
            ErrorKeys.invalidInputErrorMessage(exactNumberOfPartners),
            _.isValid
          )
      )
    )((a, b) => (a, b))(tuple => Some(tuple)))
      .verifying(
        error = ErrorKeys.requiredFieldErrorMessage(whichNumberType),
        constraint = {
          case (HowManyIndividualsOption.FiveOrLess, Some(_)) => true
          case (HowManyIndividualsOption.SixOrMore, _) => true
          case _ => false
        }
      )
      .transform[NumberOfPartners](
        {
          case (HowManyIndividualsOption.FiveOrLess, Some(n)) => n
          case (HowManyIndividualsOption.SixOrMore, _) => SixOrMore()
          case culprit => throw new IllegalStateException(s"Form validation failed: $culprit")
        },
        {
          case n: FiveOrLess => (HowManyIndividualsOption.FiveOrLess, Some(n))
          case _: SixOrMore => (HowManyIndividualsOption.SixOrMore, None)
        }
      )
  )
