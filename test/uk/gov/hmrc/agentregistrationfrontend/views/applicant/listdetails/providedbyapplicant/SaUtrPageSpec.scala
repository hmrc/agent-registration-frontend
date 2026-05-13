/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.providedbyapplicant

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.providedbyapplicant.SaUtrForm
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.providedbyapplicant.SaUtrPage

class SaUtrPageSpec
extends ViewSpec:

  val name: String = tdAll.providedDetails.afterAccessConfirmed.individualName.value

  val applicantProvidedDetails: ProvidedByApplicant = ProvidedByApplicant(
    tdAll.individualProvidedDetailsId,
    tdAll.individualName
  )

  val viewTemplate: SaUtrPage = app.injector.instanceOf[SaUtrPage]

  val doc: Document = Jsoup.parse(
    viewTemplate(SaUtrForm.form, applicantProvidedDetails.individualName).body
  )

  private val heading: String = s"Do you know $name’s Self Assessment Unique Taxpayer Reference?"

  "SaUtrPage" should:
    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render the correct caption" in:
      doc
        .mainContent
        .selectOrFail(captionL)
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Relevant individual details"

    "render radio buttons on the page" in:
      val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
        legend = heading,
        options = List(
          YesNo.Yes.toString -> "Yes",
          YesNo.No.toString -> "No"
        ),
        hint = None
      )
      doc.mainContent.extractRadioGroup() shouldBe expectedRadioGroup

    "have the correct hint text for the yes option" in:
      val hint = doc.getElementById(s"${SaUtrForm.saUtrKey}-hint")

      hint.text() shouldBe "This is 10 numbers, for example 1234567890."

      doc.getElementById(SaUtrForm.saUtrKey)
        .attr("aria-describedby") shouldBe s"${SaUtrForm.saUtrKey}-hint"

    "render a save and continue button" in:
      doc
        .mainContent
        .selectOrFail(s"form button[value='${SaveAndContinue.toString}']")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Save and continue"

    "render a save and come back later button" in:
      doc
        .mainContent
        .selectOrFail("a.govuk-button--secondary")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Save and come back later"

    "render the form error correctly when the form contains an error" in:
      val field = SaUtrForm.saUtrKey
      val errorMessage = "Enter their National Insurance number"
      val formWithError = SaUtrForm.form
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(formWithError, applicantProvidedDetails.individualName).body),
        heading = heading
      )
