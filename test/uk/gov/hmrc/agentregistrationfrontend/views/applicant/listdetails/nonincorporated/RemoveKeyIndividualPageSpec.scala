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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.nonincorporated

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.forms.RemoveKeyIndividualForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.nonincorporated.RemoveKeyIndividualPage

class RemoveKeyIndividualPageSpec
extends ViewSpec:

  private val formAction = AppRoutes.apply.listdetails.nonincorporated.RemoveKeyIndividualController.submit(
    tdAll.individualProvidedDetails.individualProvidedDetailsId
  )

  val heading = "Confirm that you want to remove Test Name from the list of partners"

  val viewTemplate: RemoveKeyIndividualPage = app.injector.instanceOf[RemoveKeyIndividualPage]

  "RemoveKeyIndividualPage" should {
    val doc: Document = Jsoup.parse(viewTemplate(
      form = RemoveKeyIndividualForm.form(tdAll.individualProvidedDetails.individualName.value),
      individualProvidedDetails = tdAll.individualProvidedDetails
    ).body)

    "have the correct title for '$ordinalKey'" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a form with correct action" in:
      val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe formAction.url

    "render a radio button for each option" in:
      val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
        legend = heading,
        options = List(
          "Yes" -> "Yes",
          "No" -> "No"
        ),
        hint = None
      )
      doc.mainContent.extractRadioGroup() shouldBe expectedRadioGroup

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
      val field = RemoveKeyIndividualForm.key
      val errorMessage = "Select yes if you want to remove Test Name from the list of partners"
      val formWithError = RemoveKeyIndividualForm.form(tdAll.individualProvidedDetails.individualName.value)
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(
          form = formWithError,
          individualProvidedDetails = tdAll.individualProvidedDetails
        ).body),
        heading = heading
      )
  }
