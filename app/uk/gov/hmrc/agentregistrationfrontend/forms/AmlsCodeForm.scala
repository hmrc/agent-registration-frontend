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
import play.api.data.Mapping
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.forms.mappings.Mappings

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmlsCodeForm @Inject() (amlsCodes: AmlsCodes):
  val form: Form[AmlsCode] =
    val mapping: Mapping[AmlsCode] = Mappings.textFromOptions(
      formMessageKey = AmlsCodeForm.key,
      options = amlsCodes.amlsCodes.keys.map(_.value).toSeq
    ).transform[AmlsCode](AmlsCode.apply, _.value)
    Form(Forms.single(AmlsCodeForm.key -> mapping))

object AmlsCodeForm:
  val key = "amlsSupervisoryBody"
