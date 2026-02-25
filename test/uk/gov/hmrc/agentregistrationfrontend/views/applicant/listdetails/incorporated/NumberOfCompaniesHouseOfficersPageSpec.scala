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

  // Use an application type that the page explicitly supports (LLP/Ltd/Partnership/SoleTrader).
  val agentApplication: AgentApplication =
    tdAll
      .agentApplicationLlp
      .afterHmrcStandardForAgentsAgreed

  private val entityName: String = tdAll.companyName
  private val companiesHouseOfficersCount: Int = 6

  private val caption: String = "LLP member and tax adviser information"
  private val heading: String = s"Members responsible for tax activities at $entityName"
  private val intro: String = s"There are $companiesHouseOfficersCount people listed in Companies House as members of $entityName."
  private val p1: String = "We need to know how many of these have:"
  private val question: String = "How many members are responsible for tax advice?"
  private val hint: String = s"Must be a number between 1 and $companiesHouseOfficersCount."

  private def render(form: play.api.data.Form[Int]): Document = Jsoup.parse(viewTemplate(
    form = form,
    entityName = entityName,
    agentApplication = agentApplication,
    companiesHouseOfficersCount = companiesHouseOfficersCount
  ).body)

  "NumberOfCompaniesHouseOfficersPage" should:

    val doc: Document = render(NumberCompaniesHouseOfficersForm.form(companiesHouseOfficersCount))

    "contain expected content" in:
      doc.mainContent shouldContainContent (
        s"""
           |$caption
           |$heading
           |$intro
           |$p1
           |material responsibility for how tax advice is carried out
           |significant authority over HMRC interactions
           |$question
           |$hint
           |Save and continue
           |Save and come back later
           |Is this page not working properly? (opens in new tab)
           |""".stripMargin
      )

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

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
      val errorMessage = "Enter how many members are responsible for tax advice"
      val formWithError = NumberCompaniesHouseOfficersForm
        .form(companiesHouseOfficersCount)
        .withError(field, errorMessage)

      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = render(formWithError),
        heading = heading
      )
