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

package uk.gov.hmrc.agentregistrationfrontend.forms

import play.api.data.Form
import play.api.data.Forms.{boolean, optional, single, text}
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.FormFieldHelper

object ConfirmationForm extends FormFieldHelper {
  def form(fieldName: String, args: String*): Form[Boolean] = {
    Form[Boolean](
      single(
        fieldName -> optional(boolean)
          .verifying(mandatoryBoolean(fieldName, args*))
          .transform(_.getOrElse(false), Some(_))
      )
    )
  }
}
