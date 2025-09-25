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

package uk.gov.hmrc.agentregistrationfrontend.views.register.aboutyourbusiness

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.forms.BusinessTypeSessionForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.aboutyourbusiness.BusinessTypeSessionPage

class BusinessTypeSessionPageSpec
extends ViewSpec:

  val viewTemplate: BusinessTypeSessionPage = app.injector.instanceOf[BusinessTypeSessionPage]
  implicit val doc: Document = Jsoup.parse(viewTemplate(BusinessTypeSessionForm.form).body)
  private val heading: String = "How is your business set up?"

  "BusinessTypeSessionPage" should:

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a radio button for each option" in:
      val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
        legend = heading,
        options = List(
          "Sole trader" -> "SoleTrader",
          "Limited company" -> "LimitedCompany",
          "A type of partnership" -> "PartnershipType",
          "Something else" -> "NotSupported"
        ),
        hint = None
      )
      doc.mainContent.extractRadioGroup() shouldBe expectedRadioGroup

    "render a continue button" in:
      doc.select("button[type=submit]").text() shouldBe "Continue"

    "render a form error when the form contains an error" in:
      val field = "businessType"
      val errorMessage = "Tell us how your business is set up"
      val formWithError = BusinessTypeSessionForm.form
        .withError(field, errorMessage)
      val errorDoc: Document = Jsoup.parse(viewTemplate(formWithError).body)
      errorDoc.title() shouldBe s"Error: $heading - Apply for an agent services account - GOV.UK"
      errorDoc.selectOrFail(".govuk-error-summary__title").selectOnlyOneElementOrFail().text() shouldBe "There is a problem"
      errorDoc.selectOrFail(".govuk-error-summary__list > li > a").selectOnlyOneElementOrFail().selectAttrOrFail("href") shouldBe s"#$field"
      errorDoc.selectOrFail(".govuk-error-message").selectOnlyOneElementOrFail().text() shouldBe s"Error: $errorMessage"
