/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.forms.helpers

import play.api.data.validation.*

object FormFieldHelper {

  def constraint[T](
    constraint: (T => Boolean),
    error: => String,
    args: String*
  ): Constraint[T] = Constraint((t: T) =>
    if (constraint(t))
      Valid
    else
      Invalid(Seq(ValidationError(error, args)))
  )

  def mandatoryBoolean(
    errorMessageKey: String,
    args: String*
  ): Constraint[Option[Boolean]] = Constraint[Option[Boolean]] { fieldValue =>
    if (fieldValue.isDefined)
      Valid
    else
      Invalid(ValidationError(mandatoryFieldErrorMessage(errorMessageKey), args*))
  }

  def mandatoryRadio(
    errorMessageKey: String,
    options: Seq[String],
    args: String*
  ): Constraint[Option[String]] = Constraint[Option[String]] { fieldValue =>
    if (fieldValue.isDefined && options.contains(fieldValue.get))
      Valid
    else
      Invalid(ValidationError(mandatoryFieldErrorMessage(errorMessageKey), args*))
  }

  def invalidMandatoryField(
    messageKey: String,
    inputFieldClass: String
  ): Invalid = {
    Invalid(ValidationError(mandatoryFieldErrorMessage(messageKey), "inputFieldClass" -> inputFieldClass))
  }

  def invalidInput(
    messageKey: String,
    inputFieldClass: String
  ): Invalid = {
    Invalid(ValidationError(invalidInputErrorMessage(messageKey), "inputFieldClass" -> inputFieldClass))
  }

  def mandatoryFieldErrorMessage(messageKey: String): String = s"$messageKey.error.required"

  def invalidInputErrorMessage(messageKey: String): String = s"$messageKey.error.invalid"

}
