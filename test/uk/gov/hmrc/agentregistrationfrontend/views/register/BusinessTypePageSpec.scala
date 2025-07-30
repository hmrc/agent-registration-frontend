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

package uk.gov.hmrc.agentregistrationfrontend.views.register

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.forms.BusinessTypeForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpecSupport
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.BusinessTypePage

class BusinessTypePageSpec
extends ViewSpecSupport:

  val viewTemplate: BusinessTypePage = app.injector.instanceOf[BusinessTypePage]
  implicit val doc: Document = Jsoup.parse(viewTemplate(BusinessTypeForm.form).body)
  private val heading: String = "How is your business set up?"

  "BusinessTypePage" should:

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a radio button for each option" in:
      val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
        legend = heading,
        options = List(
          "Sole trader" -> "SoleTrader",
          "Limited company" -> "LimitedCompany",
          "Partnership" -> "GeneralPartnership",
          "Limited liability partnership" -> "LimitedLiabilityPartnership"
        ),
        hint = None
      )
      doc.mainContent.extractRadios(1).value shouldBe expectedRadioGroup

    "render a details element with content for when the business type is not listed" in:
      val expectedSummary = "The business is set up as something else"
      val expectedDetails =
        "To get an agent services account your business must be a sole trader, limited company, partnership or limited liability partnership."
      val expectedLinkText = "Finish and sign out"
      val details = doc.select("details")
      details.size() shouldBe 1
      details.text() shouldBe s"$expectedSummary $expectedDetails $expectedLinkText"

    "render a save and continue button" in:
      doc.select("button[type=submit]").text() shouldBe "Save and continue"

    "render a form error when the form contains an error" in:
      val field = "businessType"
      val errorMessage = "Tell us how your business is set up"
      val formWithError = BusinessTypeForm.form
        .withError(field, errorMessage)
      val errorDoc: Document = Jsoup.parse(viewTemplate(formWithError).body)
      errorDoc.title() shouldBe s"Error: $heading - Apply for an agent services account - GOV.UK"
      errorDoc.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
      errorDoc.select(".govuk-error-summary__list > li > a").attr("href") shouldBe s"#$field"
      errorDoc.select(".govuk-error-message").text() shouldBe s"Error: $errorMessage"
