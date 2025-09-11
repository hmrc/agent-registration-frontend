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
import play.api.data.FormError
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec

class AmlsCodeFormSpec
extends ISpec:

  "AmlsCodeForm" should:
    val amlsCodeForm: AmlsCodeForm = app.injector.instanceOf[AmlsCodeForm]
    "return a form with data when bind succeeds" in:
      val formBound: Form[AmlsCode] = amlsCodeForm.form.bind(Map("amlsSupervisoryBody" -> "ATT"))
      formBound.errors shouldBe Nil
      formBound.value.value shouldBe AmlsCode("ATT")

    "return a form with errors when bind fails" in:
      val formBound: Form[AmlsCode] = amlsCodeForm.form.bind(Map("amlsSupervisoryBody" -> "NOT_ON_THE_LIST"))
      formBound.errors shouldBe List(FormError(
        "amlsSupervisoryBody",
        List("amlsSupervisoryBody.error.invalid"),
        Seq(Seq())
      ))
      formBound.value shouldBe None

    "return a form with errors when no data in bind" in:
      val formBound: Form[AmlsCode] = amlsCodeForm.form.bind(Map())
      formBound.errors shouldBe List(FormError(
        "amlsSupervisoryBody",
        List("amlsSupervisoryBody.error.required"),
        Seq()
      ))
      formBound.value shouldBe None
