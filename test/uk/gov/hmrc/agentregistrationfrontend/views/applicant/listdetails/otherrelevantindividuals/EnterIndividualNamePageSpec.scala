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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.otherrelevantindividuals

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.forms.OtherRelevantIndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.otherrelevantindividuals.EnterIndividualNamePage

class EnterIndividualNamePageSpec
extends ViewSpec:

  private val formAction = AppRoutes.apply.listdetails.otherrelevantindividuals.EnterOtherRelevantIndividualController.submit

  private val viewTemplate: EnterIndividualNamePage = app.injector.instanceOf[EnterIndividualNamePage]

  "EnterIndividualNamePage" should {

    val doc: Document = Jsoup.parse(viewTemplate(
      form = OtherRelevantIndividualNameForm.form,
      formAction = formAction,
      ordinalKey = "first"
    ).body)

    val expectedHeading = messages("otherRelevantIndividualName.label.first")

    "have the correct title" in:
      doc.title() shouldBe s"$expectedHeading - Apply for an agent services account - GOV.UK"

    "render the partnership caption" in:
      doc
        .mainContent
        .selectOrFail("h2.govuk-caption-l")
        .selectOnlyOneElementOrFail()
        .text() shouldBe messages("lists.caption.title.Partnership")

    "render a form with an input of type text" in:
      val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe formAction.url

      form
        .selectOrFail("label[for=otherRelevantIndividualName]")
        .selectOnlyOneElementOrFail()
        .text() shouldBe expectedHeading

      form
        .selectOrFail("input[name=otherRelevantIndividualName][type=text]")
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

    "render a form error when the form contains an error" in:
      val field = OtherRelevantIndividualNameForm.key
      val errorMessage = "Enter the full name of the person"

      val formWithError = OtherRelevantIndividualNameForm.form.withError(field, errorMessage)

      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(
          form = formWithError,
          formAction = formAction,
          ordinalKey = "first"
        ).body),
        heading = expectedHeading
      )
  }
