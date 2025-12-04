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
import play.api.data.Forms.text
import uk.gov.voa.play.form.ConditionalMappings.isEqual
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIf
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.llp.MemberNino
import uk.gov.hmrc.agentregistrationfrontend.forms.formatters.TextFormatter
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.ErrorKeys

object MemberNinoForm:

  private val yes: String = YesNo.Yes.toString
  private val no: String = YesNo.No.toString

  val hasNinoKey = "memberNino.hasNino"
  val ninoKey = "memberNino.nino"

  val form: Form[MemberNino] = Form(
    mapping(
      hasNinoKey -> Forms.of(TextFormatter(ErrorKeys.requiredFieldErrorMessage(hasNinoKey)))
        .verifying(
          ErrorKeys.requiredFieldErrorMessage(hasNinoKey),
          _.nonEmpty
        ),
      ninoKey -> mandatoryIf(
        isEqual(hasNinoKey, yes),
        text
          .verifying(
            ErrorKeys.requiredFieldErrorMessage(ninoKey),
            _.nonEmpty
          )
          .verifying(
            ErrorKeys.invalidInputErrorMessage(ninoKey),
            value => Nino.isValid(value)
          )
      )
    )(
      (
        hasNinoStr,
        ninoStrOpt
      ) =>
        (hasNinoStr, ninoStrOpt) match
          case (x, Some(ninoStr)) if x.equals(yes) => MemberNino.Provided(Nino(ninoStr))
          case _ => MemberNino.NotProvided
    ) {
      case MemberNino.Provided(nino) => Some((yes, Some(nino.value)))
      case MemberNino.NotProvided => Some((no, None))
      case MemberNino.FromAuth(nino) => Some((yes, Some(nino.value)))
    }
  )
