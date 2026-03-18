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
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.individual.ConfirmNameMatchForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.ConfirmNameMatchPage

class ConfirmNameMatchPageSpec
extends ViewSpec:

  val linkId: LinkId = tdAll.linkId
  val viewTemplate: ConfirmNameMatchPage = app.injector.instanceOf[ConfirmNameMatchPage]
  val doc: Document = Jsoup.parse(viewTemplate(
    form = ConfirmNameMatchForm.form,
    individualProvidedDetails = tdAll.providedDetails.precreated,
    linkId = linkId
  ).body)
  private val heading: String = "Are these details correct?"
  private val yesNoLabel: String = "Are these your details?"

  "ConfirmNameMatchPage" should:
    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a summary list with the correct name" in:
      val expectedSummaryList: TestReadOnlySummaryList = TestReadOnlySummaryList(
        List(TestReadOnlySummaryRow(
          key = "Name",
          value = "Test Name"
        ))
      )
      doc.mainContent.extractReadOnlySummaryList() shouldBe expectedSummaryList

    "render a radio button for each option" in:
      val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
        legend = yesNoLabel,
        options = List(
          "Yes" -> YesNo.Yes.toString,
          "No" -> YesNo.No.toString
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

  "render the form error correctly when the form contains an error" in:
    val field = ConfirmNameMatchForm.key
    val errorMessage = "Select yes if these are your details"
    val formWithError = ConfirmNameMatchForm.form
      .withError(field, errorMessage)
    behavesLikePageWithErrorHandling(
      field = field,
      errorMessage = errorMessage,
      errorDoc = Jsoup.parse(viewTemplate(
        form = formWithError,
        individualProvidedDetails = tdAll.providedDetails.precreated,
        linkId = linkId
      ).body),
      heading = heading
    )
