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

package uk.gov.hmrc.agentregistrationfrontend.forms.mappings

import play.api.data.Form
import play.api.data.FormError
import play.api.data.Mapping
import play.api.data.Forms.single
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec

import java.time.LocalDate

class MappingsSpec
extends UnitSpec:

  "text mapping" should:
    val mapping: Mapping[String] = Mappings.text("message-key")
    val form: Form[String] = Form("text" -> mapping)

    "return a valid text for any string" in {
      val formBound = form.bind(Map("text" -> "example text"))
      formBound.hasErrors shouldBe false
      formBound.value shouldBe Some("example text")
    }

    "return a `message-key.error.required` error when no string provided" in:
      val formBound: Form[String] = form.bind(Map())
      formBound.hasErrors shouldBe true
      formBound.errors shouldBe List(FormError(
        key = "text",
        messages = List("message-key.error.required"),
        args = Seq()
      ))
      formBound.value shouldBe None

  "textFromOptions" should:
    val mapping: Mapping[String] = Mappings.textFromOptions(formMessageKey = "message-key", options = Seq("a", "b", "c"))
    val form: Form[String] = Form("text" -> mapping)

    "return a valid text for any string from available options" in:
      Seq("a", "b", "c").foreach: validSelection =>
        val formBound = form.bind(Map("text" -> validSelection))
        formBound.hasErrors shouldBe false
        formBound.value shouldBe Some(validSelection)

    "return a `message-key.error.invalid` error when invalid selection provided" in:
      val formBound = form.bind(Map("text" -> "d"))
      formBound.hasErrors shouldBe true
      formBound.errors shouldBe List(FormError(
        key = "text",
        messages = List("message-key.error.invalid"),
        args = Seq(Seq())
      ))
      formBound.value shouldBe None

  "The localDate form mapping" should:

    val mapping = Mappings.localDate("message-key")
    val form = Form(single("date" -> mapping))
    val exampleData = Map(
      "date.day" -> "1",
      "date.month" -> "1",
      "date.year" -> "2020"
    )

    "return a valid result when binding with appropriate day/month/year fields" in:
      val formBound: Form[LocalDate] = form.bind(exampleData)
      formBound.hasErrors shouldBe false
      formBound.value shouldBe Some(LocalDate.parse("2020-01-01"))

    "return a validation error when binding without day/month/year fields" in:
      val formBound = form.bind(Map("date.something" -> "1"))
      formBound.hasErrors shouldBe true
      formBound.errors shouldBe List(FormError(
        key = "date",
        messages = List("message-key.error.required"),
        args = Seq()
      ))

    "parse and transform a valid date when it is filled to the form" in:
      form.fill(LocalDate.parse("2020-01-01")).data shouldBe exampleData
