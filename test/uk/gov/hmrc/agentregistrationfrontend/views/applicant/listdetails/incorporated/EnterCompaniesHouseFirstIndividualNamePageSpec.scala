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
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.EnterCompaniesHouseFirstIndividualNamePage

class EnterCompaniesHouseFirstIndividualNamePageSpec
extends ViewSpec:

  val viewTemplate: EnterCompaniesHouseFirstIndividualNamePage = app.injector.instanceOf[EnterCompaniesHouseFirstIndividualNamePage]

  private val entityName: String = tdAll.companyName
  private val sixOrMoreOfficers: SixOrMoreOfficers = TestOnlyData.sixOrMoreCompaniesHouseOfficers
  private val numberOfMembers: Int = sixOrMoreOfficers.numberOfOfficersResponsibleForTaxMatters
  private val paddingRequired: Int = sixOrMoreOfficers.requiredPadding
  private val ordinalKey: String = "first"
  private val formAction: play.api.mvc.Call = AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.submitSixOrMore
  private val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterHmrcStandardForAgentsAgreed

  private val caption: String = "LLP members and other tax adviser information"
  private val heading: String = s"Tell us about the members of $entityName"
  private val p1: String = "We'll check this against the business records in Companies House."
  private val introLine: String = "We need the names of:"
  private val bullet1: String = s"the $numberOfMembers LLP members responsible for tax advice"
  private val bullet2: String = s"$paddingRequired other LLP member"
  private val detailsSummary: String = "What we mean by responsible for tax advice"
  private val detailsContent: String = "An LLP member is responsible for tax advice if they have:"
  private val question: String = "What is the name of the first person?"

  private def render(form: play.api.data.Form[IndividualName]): Document = Jsoup.parse(viewTemplate(
    form = form,
    entityName = entityName,
    sixOrMoreOfficers = sixOrMoreOfficers,
    ordinalKey = ordinalKey,
    formAction = formAction,
    agentApplication = agentApplication
  ).body)

  "EnterCompaniesHouseFirstIndividualNamePage" should:

    val doc: Document = render(CompaniesHouseIndividuaNameForm.form)

    "contain expected content" in:
      doc.mainContent shouldContainContent (
        s"""
           |$caption
           |$heading
           |$p1
           |$introLine
           |$bullet1
           |$bullet2
           |$detailsSummary
           |$detailsContent
           |material responsibility for tax advice activities
           |significant authority over HMRC interactions
           |$question
           |First names
           |Last name
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
        errorDoc = render(formWithError),
        heading = heading
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
        errorDoc = render(formWithError),
        heading = heading
      )
