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
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.providedbyapplicant.ApplicantProvidedNinoForm
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.providedbyapplicant.ApplicantProvidedNinoPage
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo

class ApplicantProvidedNinoPageSpec
extends ViewSpec:

  val name: String = tdAll.providedDetails.afterAccessConfirmed.individualName.value

  val applicantProvidedDetails: ProvidedByApplicant = ProvidedByApplicant(
    tdAll.individualProvidedDetailsId,
    tdAll.individualName
  )

  val viewTemplate: ApplicantProvidedNinoPage = app.injector.instanceOf[ApplicantProvidedNinoPage]

  val doc: Document = Jsoup.parse(
    viewTemplate(ApplicantProvidedNinoForm.form, Some(applicantProvidedDetails.individualName.value)).body
  )

  private val heading: String = s"Do you know $name’s National Insurance Number?"

  "ApplicantProvidedDoBPage" should:
    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render the correct caption" in:
      doc
        .mainContent
        .selectOrFail("h2.govuk-caption-l")
        .selectOnlyOneElementOrFail()
        .text() shouldBe messages("applicant-provided.date-of-birth.caption")

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
      val hint = doc.getElementById("applicant-provided.nino-hint")

      hint.text() shouldBe "For example, QQ 12 34 56 C."

      doc.getElementById("applicant-provided.nino")
        .attr("aria-describedby") shouldBe "applicant-provided.nino-hint"

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
      val field = ApplicantProvidedNinoForm.ninoKey
      val errorMessage = "Enter their National Insurance number"
      val formWithError = ApplicantProvidedNinoForm.form
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(formWithError, Some(applicantProvidedDetails.individualName.value)).body),
        heading = heading
      )
