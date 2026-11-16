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
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.validation.ValidationError
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.ErrorKeys

object NumberOfPartnersResponsibleForTaxForm:

  val numberOfPartnersResponsibleForTax: String = "numberOfPartnersResponsibleForTax"

  def form: Form[Int] = Form(
    mapping(
      numberOfPartnersResponsibleForTax ->
        text
          .verifying(
            ErrorKeys.requiredFieldErrorMessage(s"$numberOfPartnersResponsibleForTax"),
            _.nonEmpty
          )
          .verifying(
            s"$numberOfPartnersResponsibleForTax.notANumber",
            str => str.toIntOption.isDefined
          )
          .transform[Int](_.toInt, _.toString)
          .verifying(
            Constraint[Int](ErrorKeys.invalidInputErrorMessage(s"$numberOfPartnersResponsibleForTax")) { n =>
              if (n >= 1 && n <= 30)
                Valid
              else
                Invalid(ValidationError(
                  ErrorKeys.invalidInputErrorMessage(s"$numberOfPartnersResponsibleForTax"),
                  30
                ))
            }
          )
    )(identity)(Some(_))
  )
