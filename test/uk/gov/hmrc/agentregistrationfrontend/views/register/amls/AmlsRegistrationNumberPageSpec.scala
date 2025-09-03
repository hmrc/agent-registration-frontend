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

package uk.gov.hmrc.agentregistrationfrontend.views.register.amls

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsRegistrationNumberForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.amls.AmlsRegistrationNumberPage

class AmlsRegistrationNumberPageSpec
extends ViewSpec:

  val viewTemplate: AmlsRegistrationNumberPage = app.injector.instanceOf[AmlsRegistrationNumberPage]

  val doc: Document = Jsoup.parse(
    viewTemplate(
      AmlsRegistrationNumberForm(isHmrc = false).form
    ).body
  )
  private val heading: String = "What is your registration number?"

  "AmlsRegistrationNumberPage view" should:

    "contain expected content" in:
      doc.mainContent shouldContainContent
        """
          |Anti-money laundering supervision details
          |What is your registration number?
          |Save and continue
          |Save and come back later
          |""".stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe heading

    "have only one text input element in the form" in:
//      doc
//        .mainContent
//        .selectOrFail("form input[type='text']")
//        .selectOnlyOneElementOrFail()
//        .toInputField shouldBe TestInputField("xxx", None, "xxx")
      // TODO: toInputFiels isn't working, discuss how it should behave

      doc
        .mainContent
        .selectOrFail("form input[type='text']")
        .selectOnlyOneElementOrFail()

    "render a save and continue button" in:
      doc
        .mainContent
        .selectOrFail(s"form button[value='${SaveAndContinue.toString}']")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Save and continue"

    "render a save and come back later button" in:
      doc
        .mainContent
        .selectOrFail(s"form button[value=${SaveAndComeBackLater.toString}]")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Save and come back later"

    "render an error message when form has errors" in:
      val key: String = AmlsRegistrationNumberForm.key
      val errorMessage: String = "Enter your registration number"
      val formWithError: Form[AmlsRegistrationNumber] = AmlsRegistrationNumberForm(isHmrc = false).form
        .withError(key, errorMessage)
      val errorDoc: Document = Jsoup.parse(viewTemplate(formWithError).body)
      errorDoc.mainContent shouldContainContent
        """
          |There is a problem
          |Enter your registration number
          |Anti-money laundering supervision details
          |What is your registration number?
          |Error:
          |Enter your registration number
          |Save and continue
          |Save and come back later
          |""".stripMargin

      errorDoc.title() shouldBe s"Error: $heading - Apply for an agent services account - GOV.UK"
      errorDoc.selectOrFail(".govuk-error-summary__title").selectOnlyOneElementOrFail().text() shouldBe "There is a problem"
      errorDoc.selectOrFail(".govuk-error-summary__list > li > a").selectOnlyOneElementOrFail().selectAttrOrFail("href") shouldBe s"#$key"
      errorDoc.selectOrFail(".govuk-error-message").selectOnlyOneElementOrFail().text() shouldBe s"Error: $errorMessage"
