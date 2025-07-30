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

import play.api.data.Forms.mapping
import play.api.data.{FieldMapping, Form, Forms}
import uk.gov.hmrc.agentregistration.shared.util.EnumFormatter
import uk.gov.hmrc.agentregistration.shared.BusinessType

object BusinessTypeForm:

  val form: Form[BusinessType] =
    val fieldMapping: FieldMapping[BusinessType] = Forms.of(EnumFormatter.formatter[BusinessType](
      errorMessageIfMissing = "businessType.error.required",
      errorMessageIfEnumError = "businessType.error.invalid"
    ))
    Form(
      mapping = mapping("businessType" -> fieldMapping)(identity)(Some(_))
    )
