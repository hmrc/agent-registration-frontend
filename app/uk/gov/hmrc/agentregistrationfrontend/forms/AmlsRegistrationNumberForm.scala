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
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber

object AmlsRegistrationNumberForm:
  val key: String = "amlsRegistrationNumber"

class AmlsRegistrationNumberForm(isHmrc: Boolean)(implicit messages: Messages) {

  import AmlsRegistrationNumberForm.*

  // these methods could end up being needed in other forms, until then this is the only place they are used
  private def stripWhiteSpaces(str: String): String = str.trim.replaceAll("\\s", "")
  private def validateText(messageKey: String): Constraint[Option[String]] = {
    Constraint[Option[String]] { (userInput: Option[String]) =>
      userInput match {
        case Some(value) if stripWhiteSpaces(value).nonEmpty => Valid
        case _ => Invalid(ValidationError(messages(messageKey)))
      }
    }
  }

  val form: Form[AmlsRegistrationNumber] = Form[AmlsRegistrationNumber](
    key -> optional(text)
      .verifying(validateText(s"$key.error.required"))
      .verifying(
        s"$key.error.invalid",
        value =>
          val normalisedValue = stripWhiteSpaces(value.getOrElse(""))
          if (isHmrc)
            AmlsRegistrationNumber.isValidForHmrc(normalisedValue)
          else
            AmlsRegistrationNumber.isValidForNonHmrc(normalisedValue)
      )
      .transform[AmlsRegistrationNumber](
        userInput => AmlsRegistrationNumber(userInput.getOrElse("").trim),
        amlsRegistrationNumber => Some(amlsRegistrationNumber.value)
      )
  )

}
