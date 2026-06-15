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
import play.api.data.Forms.optional
import play.api.data.Forms.text
import play.api.data.Mapping
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.validation.ValidationError
import uk.gov.hmrc.agentregistration.shared.amls.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.amls.AmlsSupervisoryBodyCode
import uk.gov.hmrc.agentregistration.shared.util.StringExtensions.canonicalise
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.ErrorKeys

object AmlsRegistrationNumberForm:
  val key: String = "amlsRegistrationNumber"

class AmlsRegistrationNumberForm(supervisoryBody: AmlsSupervisoryBodyCode):

  import AmlsRegistrationNumberForm.*

  private def validateText(messageKey: String): Constraint[Option[String]] = Constraint[Option[String]] { (userInput: Option[String]) =>
    userInput match
      case Some(value) if value.canonicalise.nonEmpty => Valid
      case _ => Invalid(ValidationError(messageKey))
  }

  val form: Form[AmlsRegistrationNumber] =
    val mappings: Mapping[AmlsRegistrationNumber] = optional(text)
      .verifying(validateText(ErrorKeys.requiredFieldErrorMessage(key)))
      .verifying(
        s"$key.error.invalid",
        value =>
          val normalisedValue = value.getOrElse("").canonicalise
          AmlsRegistrationNumber.isValidForChosenSupervisoryBody(value = normalisedValue, supervisoryBody = supervisoryBody)
      )
      .transform[AmlsRegistrationNumber](
        userInput => AmlsRegistrationNumber(userInput.getOrElse("").canonicalise),
        amlsRegistrationNumber => Some(amlsRegistrationNumber.value)
      )
    Form[AmlsRegistrationNumber](key -> mappings)
