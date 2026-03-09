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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.incorporated

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.forms.ConfirmCompaniesHouseOfficersForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.ConfirmCompaniesHouseOfficersPage

class ConfirmCompaniesHouseOfficersPageSpec
extends ViewSpec:

  val viewTemplate: ConfirmCompaniesHouseOfficersPage = app.injector.instanceOf[ConfirmCompaniesHouseOfficersPage]

  private val entityName: String = tdAll.companyName
  private val individualNameList: Seq[IndividualName] = Seq(
    IndividualName("Tester, John"),
    IndividualName("Tester, Alice")
  )
  private val key: String = ConfirmCompaniesHouseOfficersForm.isCompaniesHouseOfficersListCorrect

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
      caption = "LLP members and other tax adviser information",
      entityType = "members",
      heading = s"Check this list of members for $entityName",
      intro = s"These are the members listed in Companies House for $entityName:",
      question = "Is this list of members correct?"
    ),
    BusinessTypeTestCase(
      label = "LimitedCompany",
      agentApplication = tdAll.agentApplicationLimitedCompany.afterHmrcStandardForAgentsAgreed,
      caption = "Directors and other tax adviser information",
      entityType = "directors",
      heading = s"Check this list of directors for $entityName",
      intro = s"These are the directors listed in Companies House for $entityName:",
      question = "Is this list of directors correct?"
    ),
    BusinessTypeTestCase(
      label = "LimitedPartnership",
      agentApplication = tdAll.agentApplicationLimitedPartnership.afterHmrcStandardForAgentsAgreed,
      caption = "Partners and other tax adviser information",
      entityType = "partners",
      heading = s"Check this list of partners for $entityName",
      intro = s"These are the partners listed in Companies House for $entityName:",
      question = "Is this list of partners correct?"
    ),
    BusinessTypeTestCase(
      label = "ScottishLimitedPartnership",
      agentApplication = tdAll.agentApplicationScottishLimitedPartnership.afterHmrcStandardForAgentsAgreed,
      caption = "Partners and other tax adviser information",
      entityType = "partners",
      heading = s"Check this list of partners for $entityName",
      intro = s"These are the partners listed in Companies House for $entityName:",
      question = "Is this list of partners correct?"
    )
  )

  private def render(
    form: play.api.data.Form[Boolean],
    agentApplication: AgentApplication
  ): Document = Jsoup.parse(viewTemplate(
    form = form,
    entityName = entityName,
    agentApplication = agentApplication,
    individualNameList = individualNameList
  ).body)

  for testCase <- testCases do
    s"ConfirmCompaniesHouseOfficersPage for ${testCase.label}" should:

      val doc: Document = render(ConfirmCompaniesHouseOfficersForm.form, testCase.agentApplication)

      "have the correct caption" in:
        doc.mainContent.select("h2.govuk-caption-l").text() shouldBe testCase.caption

      "have the correct heading" in:
        doc.mainContent.select("h1").text() shouldBe testCase.heading

      "have the correct title" in:
        doc.title() shouldBe s"${testCase.heading} - Apply for an agent services account - GOV.UK"

      "show the intro text" in:
        doc.mainContent.select("p.govuk-body").text() should include(testCase.intro)

      "show the officer names" in:
        doc.mainContent.select("ul.govuk-list--bullet").text() should include("Tester, John")
        doc.mainContent.select("ul.govuk-list--bullet").text() should include("Tester, Alice")

      "show the question" in:
        doc.mainContent.select("fieldset legend").text() shouldBe testCase.question

      "render a form with correct action" in:
        val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
        form.attr("method") shouldBe "POST"
        form.attr("action") shouldBe AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.submitFiveOrLess.url

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
        val field = key
        val errorMessage = "Select yes if this list is correct"
        val formWithError = ConfirmCompaniesHouseOfficersForm
          .form
          .withError(field, errorMessage)

        behavesLikePageWithErrorHandling(
          field = field,
          errorMessage = errorMessage,
          errorDoc = render(formWithError, testCase.agentApplication),
          heading = testCase.heading
        )
