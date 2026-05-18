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
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers
import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseIndividuaNameForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.EnterCompaniesHouseFirstIndividualNamePage

class EnterCompaniesHouseFirstIndividualNamePageSpec
extends ViewSpec:

  val viewTemplate: EnterCompaniesHouseFirstIndividualNamePage = app.injector.instanceOf[EnterCompaniesHouseFirstIndividualNamePage]

  private val entityName: String = tdAll.companyName
  private val sixOrMoreOfficers: SixOrMoreOfficers = tdAll.sixOrMoreCompaniesHouseOfficers
  private val ordinalKey: String = "first"
  private val formAction: play.api.mvc.Call = AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.submitSixOrMore

  private def render(
    form: play.api.data.Form[IndividualName],
    agentApplication: AgentApplication
  ): Document = Jsoup.parse(viewTemplate(
    form = form,
    entityName = entityName,
    sixOrMoreOfficers = sixOrMoreOfficers,
    ordinalKey = ordinalKey,
    formAction = formAction,
    agentApplication = agentApplication
  ).body)

  case class BusinessTypeTestCase(
    label: String,
    agentApplication: AgentApplication,
    caption: String,
    headingEntityType: String,
    bullet1EntityType: String,
    bullet2EntityType: String
  )

  private val testCases = Seq(
    BusinessTypeTestCase(
      label = "LimitedLiabilityPartnership",
      agentApplication = tdAll.agentApplicationLlp.afterHmrcStandardForAgentsAgreed,
      caption = "LLP members and other relevant individuals",
      headingEntityType = "LLP members",
      bullet1EntityType = "LLP members",
      bullet2EntityType = "LLP member"
    ),
    BusinessTypeTestCase(
      label = "LimitedCompany",
      agentApplication = tdAll.agentApplicationLimitedCompany.afterHmrcStandardForAgentsAgreed,
      caption = "Directors and other relevant individuals",
      headingEntityType = "directors",
      bullet1EntityType = "directors",
      bullet2EntityType = "director"
    ),
    BusinessTypeTestCase(
      label = "Partnership",
      agentApplication = tdAll.agentApplicationLimitedPartnership.afterHmrcStandardForAgentsAgreed,
      caption = "Partners and other relevant individuals",
      headingEntityType = "partners",
      bullet1EntityType = "partners",
      bullet2EntityType = "partner"
    )
  )

  for testCase <- testCases do
    s"EnterCompaniesHouseFirstIndividualNamePage for ${testCase.label}" should:

      val headingWithPadding = s"Tell us about 5 ${testCase.headingEntityType} at $entityName"
      val doc: Document = render(CompaniesHouseIndividuaNameForm.form, testCase.agentApplication)

      "have the correct caption" in:
        doc.mainContent.select(captionL).text() shouldBe testCase.caption

      "have the correct heading" in:
        doc.mainContent.select("h1").text() shouldBe headingWithPadding

      "have the correct title" in:
        doc.title() shouldBe s"$headingWithPadding - Apply for an agent services account - GOV.UK"

      "show the correct p1 text" in:
        doc.mainContent.select("p.govuk-body").first().text() shouldBe
          "We need the names of:"

      "show the correct padding breakdown bullet1" in:
        doc.mainContent.select("ul.govuk-list--bullet li").first().text() should include(testCase.bullet1EntityType)

      "show the correct padding breakdown bullet2" in:
        doc.mainContent.select("ul.govuk-list--bullet li").get(1).text() should include(testCase.bullet2EntityType)

      "render a form with correct action" in:
        val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
        form.attr("method") shouldBe "POST"
        form.attr("action") shouldBe formAction.url

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

      "render a form error when the firstName field contains an error" in:
        val field = CompaniesHouseIndividuaNameForm.firstNameKey
        val errorMessage = "Enter first name"
        val formWithError = CompaniesHouseIndividuaNameForm
          .form
          .withError(field, errorMessage)

        behavesLikePageWithErrorHandling(
          field = field,
          errorMessage = errorMessage,
          errorDoc = render(formWithError, testCase.agentApplication),
          heading = headingWithPadding
        )

      "render a form error when the lastName field contains an error" in:
        val field = CompaniesHouseIndividuaNameForm.lastNameKey
        val errorMessage = "Enter last name"
        val formWithError = CompaniesHouseIndividuaNameForm
          .form
          .withError(field, errorMessage)

        behavesLikePageWithErrorHandling(
          field = field,
          errorMessage = errorMessage,
          errorDoc = render(formWithError, testCase.agentApplication),
          heading = headingWithPadding
        )
