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
import play.api.data.Forms
import play.api.data.Forms.mapping
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.StringExtensions.canonicalise
import uk.gov.hmrc.agentregistrationfrontend.forms.formatters.TextFormatter
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.ErrorKeys
import uk.gov.hmrc.agentregistrationfrontend.util.NameValidator

object OtherRelevantIndividualNameForm:

  val key: String = "otherRelevantIndividualName"

  val form: Form[IndividualName] = Form[IndividualName](
    mapping(
      key -> Forms.of(using TextFormatter(ErrorKeys.requiredFieldErrorMessage(key)))
        .transform[String](canonicalise, identity)
        .verifying(
          ErrorKeys.requiredFieldErrorMessage(key),
          _.nonEmpty
        )
        .verifying(
          ErrorKeys.inputTooLongErrorMessage(key),
          _.length <= 100
        )
        .verifying(
          ErrorKeys.invalidInputErrorMessage(key),
          NameValidator.isValid
        )
        .transform[IndividualName](IndividualName(_), _.value)
    )(identity)(Some(_))
  )
