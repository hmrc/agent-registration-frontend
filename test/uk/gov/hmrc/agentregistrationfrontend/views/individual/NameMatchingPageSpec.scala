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

package uk.gov.hmrc.agentregistrationfrontend.views.individual

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.forms.individual.NameMatchingForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.NameMatchingPage
import uk.gov.hmrc.agentregistrationfrontend.testsupport

class NameMatchingPageSpec
extends ViewSpec:

  private val viewTemplate: NameMatchingPage = app.injector.instanceOf[NameMatchingPage]
  private val linkId = tdAll.linkId

  private val doc: Document = Jsoup.parse(viewTemplate(NameMatchingForm.form, linkId).body)
  private val heading: String = "Enter your full name"

  "NameMatchingPage" should:
    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render an input box" in:
      val expectedInputField: TestInputField = TestInputField(
        label = "Enter your full name",
        hint = None,
        inputName = "individualNameForSearch"
      )
      doc.mainContent.extractInputField() shouldBe expectedInputField

  "render a submit button" in:
    doc
      .mainContent
      .selectOrFail(s"form button[type='submit']")
      .selectOnlyOneElementOrFail()
      .text() shouldBe "Continue"

  "render the form error correctly when the form contains an error" in:
    val field = NameMatchingForm.nameSearchKey
    val errorMessage = "Error: Enter the name you provided to your agent for your application"
    val formWithError = NameMatchingForm.form
      .withError(field, errorMessage)
    behavesLikePageWithErrorHandling(
      field = field,
      errorMessage = errorMessage,
      errorDoc = Jsoup.parse(viewTemplate(formWithError, linkId).body),
      heading = heading
    )
