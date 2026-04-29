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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.providedbyapplicant

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualDateOfBirthForm
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.providedbyapplicant.ApplicantProvidedDoBPage

class ApplicantProvidedDoBPageSpec
extends ViewSpec:

  val name: String = tdAll.providedDetails.afterAccessConfirmed.individualName.value

  val applicantProvidedDetails: ProvidedByApplicant = ProvidedByApplicant(
    tdAll.individualProvidedDetailsId,
    tdAll.individualName
  )

  val viewTemplate: ApplicantProvidedDoBPage = app.injector.instanceOf[ApplicantProvidedDoBPage]

  val doc: Document = Jsoup.parse(
    viewTemplate(IndividualDateOfBirthForm.form, Some(applicantProvidedDetails.individualName.value)).body
  )

  private val heading: String = s"What is $name’s date of birth?"

  "ApplicantProvidedDoBPage" should:
    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render the correct caption" in:
      doc
        .mainContent
        .selectOrFail("h2.govuk-caption-l")
        .selectOnlyOneElementOrFail()
        .text() shouldBe messages("applicant.provided-individual-details.caption")

    "render a date input box" in:
      val expectedDateInputBox: TestDateInput = TestDateInput(
        inputName = s"What is $name’s date of birth?",
        hint = Some("For example, 31 3 1980")
      )
      doc.mainContent.extractDateInput() shouldBe expectedDateInputBox

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
