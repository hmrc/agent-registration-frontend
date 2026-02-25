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
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficer
import uk.gov.hmrc.agentregistrationfrontend.forms.ConfirmCompaniesHouseOfficersForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.ConfirmCompaniesHouseOfficersPage

class ConfirmCompaniesHouseOfficersPageSpec
extends ViewSpec:

  val viewTemplate: ConfirmCompaniesHouseOfficersPage = app.injector.instanceOf[ConfirmCompaniesHouseOfficersPage]

  // Use an incorporated application type supported by the view (LLP/Ltd/Limited Partnership/SLP).
  val agentApplication: AgentApplication =
    tdAll
      .agentApplicationLlp
      .afterHmrcStandardForAgentsAgreed

  private val entityName: String = tdAll.companyName

  private val caption: String = "LLP member and tax adviser information"
  private val heading: String = s"Check this list of members for $entityName"
  private val intro: String = s"These are the members listed in Companies House for $entityName:"
  private val question: String = "Is this list of members correct?"

  private val companiesHouseOfficers: Seq[CompaniesHouseOfficer] = Seq.empty

  private def render(form: play.api.data.Form[Boolean]): Document = Jsoup.parse(viewTemplate(
    form = form,
    entityName = entityName,
    agentApplication = agentApplication,
    companiesHouseOfficers = companiesHouseOfficers
  ).body)

  "ConfirmCompaniesHouseOfficersPage" should:

    val doc: Document = render(ConfirmCompaniesHouseOfficersForm.form)

    "contain expected content" in:
      doc.mainContent shouldContainContent (
        s"""
           |$caption
           |$heading
           |$intro
           |$question
           |Yes
           |No
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
      form.attr("action") shouldBe AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.submitFiveOrLess.url

    "render a radio button for each option" in:
      val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
        legend = question,
        options = List(
          "Yes" -> "Yes",
          "No" -> "No"
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

    "render a save and come back later button" in:
      doc
        .mainContent
        .selectOrFail(s"form button[value=${SaveAndComeBackLater.toString}]")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Save and come back later"

    "render a form error when the form contains an error" in:
      val field = ConfirmCompaniesHouseOfficersForm.isCompaniesHouseOfficersListCorrect
      val errorMessage = "Select yes if this list is correct"
      val formWithError = ConfirmCompaniesHouseOfficersForm.form.withError(field, errorMessage)

      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = render(formWithError),
        heading = heading
      )
