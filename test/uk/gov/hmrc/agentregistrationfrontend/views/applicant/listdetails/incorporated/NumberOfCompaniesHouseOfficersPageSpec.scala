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
import uk.gov.hmrc.agentregistration.shared.AgentApplication
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
    agentApplication: AgentApplication,
    caption: String,
    entityType: String,
    heading: String,
    intro: String,
    question: String
  )

  private val testCases = Seq(
    BusinessTypeTestCase(
      label = "LimitedLiabilityPartnership",
      agentApplication = tdAll.agentApplicationLlp.afterHmrcStandardForAgentsAgreed,
      caption = "LLP members and other relevant individuals",
      entityType = "members",
      heading = s"Members responsible for tax activities at $entityName",
      intro = s"There are $companiesHouseOfficersCount people listed in Companies House as members of $entityName.",
      question = "How many members are responsible for tax advice?"
    ),
    BusinessTypeTestCase(
      label = "LimitedCompany",
      agentApplication = tdAll.agentApplicationLimitedCompany.afterHmrcStandardForAgentsAgreed,
      caption = "Directors and other relevant individuals",
      entityType = "directors",
      heading = s"Directors responsible for tax activities at $entityName",
      intro = s"There are $companiesHouseOfficersCount people listed in Companies House as directors of $entityName.",
      question = "How many directors are responsible for tax advice?"
    ),
    BusinessTypeTestCase(
      label = "LimitedPartnership",
      agentApplication = tdAll.agentApplicationLimitedPartnership.afterHmrcStandardForAgentsAgreed,
      caption = "Partners and other relevant individuals",
      entityType = "partners",
      heading = s"Partners responsible for tax activities at $entityName",
      intro = s"There are $companiesHouseOfficersCount people listed in Companies House as partners of $entityName.",
      question = "How many partners are responsible for tax advice?"
    ),
    BusinessTypeTestCase(
      label = "ScottishLimitedPartnership",
      agentApplication = tdAll.agentApplicationScottishLimitedPartnership.afterHmrcStandardForAgentsAgreed,
      caption = "Partners and other relevant individuals",
      entityType = "partners",
      heading = s"Partners responsible for tax activities at $entityName",
      intro = s"There are $companiesHouseOfficersCount people listed in Companies House as partners of $entityName.",
      question = "How many partners are responsible for tax advice?"
    )
  )

  private def render(
    form: play.api.data.Form[Int],
    agentApplication: AgentApplication
  ): Document = Jsoup.parse(viewTemplate(
    form = form,
    entityName = entityName,
    agentApplication = agentApplication,
    companiesHouseOfficersCount = companiesHouseOfficersCount
  ).body)

  for testCase <- testCases do
    s"NumberOfCompaniesHouseOfficersPage for ${testCase.label}" should:

      val doc: Document = render(NumberCompaniesHouseOfficersForm.form(companiesHouseOfficersCount), testCase.agentApplication)

      "have the correct caption" in:
        doc.mainContent.select("h2.govuk-caption-l").text() shouldBe testCase.caption

      "have the correct heading" in:
        doc.mainContent.select("h1").text() shouldBe testCase.heading

      "have the correct title" in:
        doc.title() shouldBe s"${testCase.heading} - Apply for an agent services account - GOV.UK"

      "show the correct intro text" in:
        doc.mainContent.select("p.govuk-body").first().text() shouldBe testCase.intro

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
        val errorMessage = "Enter how many are responsible for tax advice"
        val formWithError = NumberCompaniesHouseOfficersForm
          .form(companiesHouseOfficersCount)
          .withError(field, errorMessage)

        behavesLikePageWithErrorHandling(
          field = field,
          errorMessage = errorMessage,
          errorDoc = render(formWithError, testCase.agentApplication),
          heading = testCase.heading
        )
