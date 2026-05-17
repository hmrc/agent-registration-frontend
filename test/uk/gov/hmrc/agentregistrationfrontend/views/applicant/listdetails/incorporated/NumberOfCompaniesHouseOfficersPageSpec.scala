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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.incorporated

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.forms.NumberCompaniesHouseOfficersForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.NumberOfCompaniesHouseOfficersPage

class NumberOfCompaniesHouseOfficersPageSpec
extends ViewSpec:

  val viewTemplate: NumberOfCompaniesHouseOfficersPage = app.injector.instanceOf[NumberOfCompaniesHouseOfficersPage]

  private val entityName: String = tdAll.companyName
  private val companiesHouseOfficersCount: Int = 6

  case class BusinessTypeTestCase(
    label: String,
    caption: String,
    officerType: String,
    heading: String,
    question: String
  )

  private val testCases = Seq(
    BusinessTypeTestCase(
      label = "LimitedLiabilityPartnership",
      caption = "LLP members and other relevant individuals",
      officerType = "LLP members",
      heading = "LLP members who are also ‘relevant individuals’",
      question = "How many members are also relevant individuals?"
    ),
    BusinessTypeTestCase(
      label = "LimitedCompany",
      caption = "Directors and other relevant individuals",
      officerType = "directors",
      heading = "Directors who are also ‘relevant individuals’",
      question = "How many directors are also relevant individuals?"
    ),
    BusinessTypeTestCase(
      label = "Partnership",
      caption = "Partners and other relevant individuals",
      officerType = "partners",
      heading = "Partners who are also ‘relevant individuals’",
      question = "How many partners are also relevant individuals?"
    )
  )

  private def render(
    form: play.api.data.Form[Int],
    businessTypeKey: String
  ): Document = Jsoup.parse(viewTemplate(
    form = form,
    entityName = entityName,
    companiesHouseOfficersCount = companiesHouseOfficersCount,
    businessTypeKey = businessTypeKey
  ).body)

  for testCase <- testCases do
    s"NumberOfCompaniesHouseOfficersPage for ${testCase.label}" should:

      val doc: Document = render(
        form = NumberCompaniesHouseOfficersForm.form(companiesHouseOfficersCount, testCase.label),
        businessTypeKey = testCase.label
      )

      "have the correct caption" in:
        doc.mainContent.select(captionL).text() shouldBe testCase.caption

      "have the correct heading" in:
        doc.mainContent.h1 shouldBe testCase.heading

      "have the correct title" in:
        doc.title() shouldBe s"${testCase.heading} - Apply for an agent services account - GOV.UK"

      "show the correct intro text" in:
        doc.mainContent.select("p.govuk-body").first().text() shouldBe s"We need to know which of the ${testCase.officerType} at $entityName meet the legal definition of relevant individuals, as defined in Chapter 226(2) of the Finance Act 2026 (opens in new tab)."

      "show the correct question" in:
        doc.mainContent.select("label.govuk-label--m").text() shouldBe testCase.question

      "render a form with correct action" in:
        val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
        form.attr("method") shouldBe "POST"
        form.attr("action") shouldBe AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.submitSixOrMore.url

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
        val field = NumberCompaniesHouseOfficersForm.numberOfOfficersResponsibleForTaxMatters
        val errorMessage = s"Enter how many ${testCase.officerType} are also relevant individuals"
        val formWithError = NumberCompaniesHouseOfficersForm
          .form(companiesHouseOfficersCount, testCase.label)
          .withError(field, errorMessage)

        behavesLikePageWithErrorHandling(
          field = field,
          errorMessage = errorMessage,
          errorDoc = render(formWithError, testCase.label),
          heading = testCase.heading
        )
