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

import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualTelephoneNumberForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.IndividualTelephoneNumberPage

class IndividualTelephoneNumberPageSpec
extends ViewSpec:

  val viewTemplate: IndividualTelephoneNumberPage = app.injector.instanceOf[IndividualTelephoneNumberPage]
  val doc: Document = Jsoup.parse(viewTemplate(IndividualTelephoneNumberForm.form).body)
  private val heading: String = "What is your telephone number?"

  "MemberTelephoneNumberPage" should:

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a form with an input of type tel for telephone number" in:
      val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe AppRoutes.providedetails.IndividualTelephoneNumberController.submit.url
      form
        .selectOrFail("label[for=individualTelephoneNumber]")
        .selectOnlyOneElementOrFail()
        .text() shouldBe heading
      form
        .selectOrFail("input[name=individualTelephoneNumber][type=tel]")
        .selectOnlyOneElementOrFail()

    "render a save and continue button" in:
      doc
        .mainContent
        .selectOrFail(s"form button[value='${SaveAndContinue.toString}']")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Save and continue"

    "render the form error correctly when the form contains an error" in:
      val field = IndividualTelephoneNumberForm.key
      val errorMessage = "Enter the number we should call to speak to you about this application"
      val formWithError = IndividualTelephoneNumberForm.form
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(formWithError).body),
        heading = heading
      )
