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
import play.api.data.Form
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseNameQuery
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.listdetails.incorporated.CompaniesHouseNameQueryForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.EnterCompaniesHouseNextIndividualNamePage

class EnterCompaniesHouseNextIndividualNamePageSpec
extends ViewSpec:

  val viewTemplate: EnterCompaniesHouseNextIndividualNamePage = app.injector.instanceOf[EnterCompaniesHouseNextIndividualNamePage]

  private val formAction: play.api.mvc.Call = AppRoutes.apply.listdetails.incoporated.EnterCompaniesHouseOfficerController.submit

  case class BusinessTypeTestCase(
    label: String,
    entityName: String,
    agentApplication: AgentApplication,
    caption: String
  )

  private val testCases = Seq(
    BusinessTypeTestCase(
      label = "LimitedLiabilityPartnership",
      entityName = tdAll.TdAgencyName.llp,
      agentApplication = tdAll.agentApplicationLlp.afterHmrcStandardForAgentsAgreed,
      caption = "LLP members and other relevant individuals"
    ),
    BusinessTypeTestCase(
      label = "LimitedCompany",
      entityName = tdAll.TdAgencyName.limitedCompany,
      agentApplication = tdAll.agentApplicationLimitedCompany.afterHmrcStandardForAgentsAgreed,
      caption = "Directors and other relevant individuals"
    ),
    BusinessTypeTestCase(
      label = "LimitedPartnership",
      entityName = tdAll.TdAgencyName.limitedPartnership,
      agentApplication = tdAll.agentApplicationLimitedPartnership.afterHmrcStandardForAgentsAgreed,
      caption = "Partners and other relevant individuals"
    ),
    BusinessTypeTestCase(
      label = "ScottishLimitedPartnership",
      entityName = tdAll.TdAgencyName.scottishLimitedPartnership,
      agentApplication = tdAll.agentApplicationScottishLimitedPartnership.afterHmrcStandardForAgentsAgreed,
      caption = "Partners and other relevant individuals"
    )
  )

  private def render(
    form: play.api.data.Form[CompaniesHouseNameQuery],
    agentApplication: AgentApplication,
    entityName: String
  ): Document = Jsoup.parse(viewTemplate(
    form = form,
    entityName = entityName,
    agentApplication = agentApplication
  ).body)

  for testCase <- testCases do
    s"EnterCompaniesHouseNextIndividualNamePage for ${testCase.label}" should:

      val p1: String = s"We’ll check this against the business records for ${testCase.entityName} in Companies House."
      val heading: String = "What is the name of the next person?"

      val doc: Document = render(
        CompaniesHouseNameQueryForm.form,
        testCase.agentApplication,
        testCase.entityName
      )

      "have the correct caption" in:
        doc.mainContent.select("h2.govuk-caption-l").text() shouldBe testCase.caption

      "have the correct heading" in:
        doc.mainContent.select("h1").text() shouldBe heading

      "have the correct title" in:
        doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

      "show the p1 text" in:
        doc.mainContent.select("p.govuk-body").first().text() shouldBe p1

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
        val errorMessage: String = "Enter your first names"
        val formWithError: Form[CompaniesHouseNameQuery] = CompaniesHouseNameQueryForm
          .form
          .bind(Map(
            CompaniesHouseNameQueryForm.firstNameKey -> "",
            CompaniesHouseNameQueryForm.lastNameKey -> "Smith"
          ))

        behavesLikePageWithErrorHandling(
          field = CompaniesHouseNameQueryForm.firstNameKey,
          errorMessage = errorMessage,
          errorDoc = render(
            formWithError,
            testCase.agentApplication,
            testCase.entityName
          ),
          heading = heading
        )

      "render a form error when the lastName field contains an error" in:
        val errorMessage: String = "Enter your last name"
        val formWithError: Form[CompaniesHouseNameQuery] = CompaniesHouseNameQueryForm
          .form
          .bind(Map(
            CompaniesHouseNameQueryForm.firstNameKey -> "John",
            CompaniesHouseNameQueryForm.lastNameKey -> ""
          ))

        behavesLikePageWithErrorHandling(
          field = CompaniesHouseNameQueryForm.lastNameKey,
          errorMessage = errorMessage,
          errorDoc = render(
            formWithError,
            testCase.agentApplication,
            testCase.entityName
          ),
          heading = heading
        )
