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

package uk.gov.hmrc.agentregistrationfrontend.forms.individual

import play.api.data.Forms.mapping
import play.api.data.Forms.text
import play.api.data.Form
import play.api.data.Forms
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.ErrorKeys

object NameMatchingForm:

  val nameSearchKey: String = "individualNameForSearch"

  private def canonicalise(name: String): String = name.trim.replaceAll("\\s+", " ")

  val form: Form[IndividualName] = Form[IndividualName](
    mapping(
      nameSearchKey -> text.transform[String](canonicalise, identity)
        .verifying(
          ErrorKeys.requiredFieldErrorMessage(nameSearchKey),
          _.nonEmpty
        )
        .verifying(
          ErrorKeys.inputTooLongErrorMessage(nameSearchKey),
          _.length <= 100
        )
        .transform[IndividualName](IndividualName(_), _.value)
        .verifying(
          ErrorKeys.invalidInputErrorMessage(nameSearchKey),
          _.isValidName
        )
    )(identity)(Some(_))
  )
