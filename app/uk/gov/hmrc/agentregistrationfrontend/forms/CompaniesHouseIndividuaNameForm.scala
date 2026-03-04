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
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.forms.formatters.TextFormatter
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.ErrorKeys

object CompaniesHouseIndividuaNameForm:

  val key: String = "companiesHouseIndividuaName"
  val firstNameKey: String = "firstName"
  val lastNameKey: String = "lastName"

  private def canonicalise(name: String): String = name.trim.replaceAll("\\s+", " ")

  val form: Form[IndividualName] = Form[IndividualName](
    mapping(
      firstNameKey -> Forms.of(TextFormatter(ErrorKeys.requiredFieldErrorMessage(firstNameKey)))
        .transform[String](canonicalise, identity)
        .verifying(
          ErrorKeys.invalidInputErrorMessage(firstNameKey),
          name => name.isEmpty || name.matches("^[a-zA-Z\\s\\-']+$")
        ),
      lastNameKey -> Forms.of(TextFormatter(ErrorKeys.requiredFieldErrorMessage(lastNameKey)))
        .transform[String](canonicalise, identity)
        .verifying(
          ErrorKeys.invalidInputErrorMessage(lastNameKey),
          name => name.isEmpty || name.matches("^[a-zA-Z\\s\\-']+$")
        )
    )(
      (
        firstName,
        lastName
      ) => IndividualName(s"$firstName $lastName")
    )(name =>
      name.value.split(" ", 2) match
        case Array(first, last) => Some((first, last))
        case Array(single) => Some((single, ""))
        case _ => Some(("", ""))
    ).verifying(
      ErrorKeys.invalidInputErrorMessage(key),
      _.isValidName
    )
  )
