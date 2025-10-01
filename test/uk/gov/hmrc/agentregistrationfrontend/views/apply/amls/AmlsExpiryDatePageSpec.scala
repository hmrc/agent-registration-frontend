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

package uk.gov.hmrc.agentregistrationfrontend.views.apply.amls

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import uk.gov.hmrc.agentregistrationfrontend.controllers.amls.routes
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsExpiryDateForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.amls.AmlsExpiryDatePage

import java.time.LocalDate

class AmlsExpiryDatePageSpec
extends ViewSpec:

  val viewTemplate: AmlsExpiryDatePage = app.injector.instanceOf[AmlsExpiryDatePage]

  val doc: Document = Jsoup.parse(
    viewTemplate(
      AmlsExpiryDateForm.form()
    ).body
  )
  private val heading: String = "When does your supervision run out?"

  "AmlsExpiryDatePage view" should:

    "contain expected content" in:
      doc.mainContent shouldContainContent
        """
          |Anti-money laundering supervision details
          |When does your supervision run out?
          |Tells us the last day you will be covered in your current 12-month supervision period. For example, 27 3 2026
          |Day
          |Month
          |Year
          |Save and continue
          |Save and come back later
          |Is this page not working properly? (opens in new tab)
          |""".stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe heading

    "render a form that posts to the correct action" in:
      val form = doc.mainContent.selectOrFail("form")
      form.attr("action").shouldBe(routes.AmlsExpiryDateController.submit.url)
      form.attr("method").shouldBe(routes.AmlsExpiryDateController.submit.method)

    "contain inputs for a date field within the form" in:
      val form = doc.mainContent.selectOrFail("form")
      val dayInputName = form
        .selectOrFail(s"input[id='${AmlsExpiryDateForm.dayKey}']")
        .selectOnlyOneElementOrFail()
        .attr("name")
      dayInputName.shouldBe(AmlsExpiryDateForm.dayKey)
      val monthInputName = form
        .selectOrFail(s"input[id='${AmlsExpiryDateForm.monthKey}']")
        .selectOnlyOneElementOrFail()
        .attr("name")
      monthInputName.shouldBe(AmlsExpiryDateForm.monthKey)
      val yearInputName = form
        .selectOrFail(s"input[id='${AmlsExpiryDateForm.yearKey}']")
        .selectOnlyOneElementOrFail()
        .attr("name")
      yearInputName.shouldBe(AmlsExpiryDateForm.yearKey)

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
      val field: String = AmlsExpiryDateForm.key
      val errorMessage: String = "Enter the date your supervision runs out"
      val formWithError: Form[LocalDate] = AmlsExpiryDateForm.form()
        .withError(field, errorMessage)
      val errorDoc: Document = Jsoup.parse(viewTemplate(formWithError).body)
      errorDoc.mainContent shouldContainContent
        """
          |There is a problem
          |Enter the date your supervision runs out
          |Anti-money laundering supervision details
          |When does your supervision run out?
          |Tells us the last day you will be covered in your current 12-month supervision period. For example, 27 3 2026
          |Error:
          |Enter the date your supervision runs out
          |Day
          |Month
          |Year
          |Save and continue
          |Save and come back later
          |Is this page not working properly? (opens in new tab)
          |""".stripMargin

      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = errorDoc,
        heading = heading,
        isWholeDateError = true
      )
