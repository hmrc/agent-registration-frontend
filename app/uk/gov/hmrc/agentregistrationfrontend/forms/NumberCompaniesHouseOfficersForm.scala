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

package uk.gov.hmrc.agentregistrationfrontend.forms

import play.api.data.Forms.mapping
import play.api.data.Form
import play.api.data.Forms
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.ErrorKeys
import uk.gov.hmrc.agentregistrationfrontend.forms.mappings.Mappings.numberFromString

object NumberCompaniesHouseOfficersForm:

  // Single input field (the value you want to capture)
  val numberOfOfficersResponsibleForTaxMatters: String = "numberOfOfficersResponsibleForTaxMatters"

  def form(totalCompaniesHouseOfficers: Int): Form[Int] = Form(
    mapping(
      numberOfOfficersResponsibleForTaxMatters ->
        numberFromString(numberOfOfficersResponsibleForTaxMatters)
          .verifying(
            ErrorKeys.invalidInputErrorMessage(numberOfOfficersResponsibleForTaxMatters),
            n => n >= 1 && n <= totalCompaniesHouseOfficers
          )
    )(identity)(Some(_))
  )
