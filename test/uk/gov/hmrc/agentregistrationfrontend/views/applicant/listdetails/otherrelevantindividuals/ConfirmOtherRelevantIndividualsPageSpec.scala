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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.otherrelevantindividuals

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.forms.ConfirmOtherRelevantIndividualsForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsPage

class ConfirmOtherRelevantIndividualsPageSpec
extends ViewSpec:

  val viewTemplate: ConfirmOtherRelevantIndividualsPage = app.injector.instanceOf[ConfirmOtherRelevantIndividualsPage]

  val agentApplication: AgentApplication =
    tdAll
      .agentApplicationGeneralPartnership
      .sectionAgentDetails
      .whenUsingExistingCompanyName
      .afterBusinessNameProvided

  private val entityName: String = tdAll.companyName
  private val heading: String = "Other people we need to know about"
  private val question: String = s"Does $entityName have any other relevant tax advisers?"

  private def render(form: play.api.data.Form[Boolean]): Document = Jsoup.parse(viewTemplate(
    form = form,
    entityName = entityName,
    agentApplication = agentApplication
  ).body)

  "ConfirmOtherRelevantIndividualsPage" should:

    val doc: Document = render(ConfirmOtherRelevantIndividualsForm.form)

    "contain expected content" in:
      doc.mainContent shouldContainContent (
        s"""
           |Partners and other relevant tax advisers
           |$heading
           |We also need to know about other relevant tax advisers.
           |This means anyone responsible for tax advice at Test Company Name, who is not an official partner.
           |Read the guidance about how HMRC defines ‘relevant tax advisers’ (opens in new tab)
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
      form.attr("action") shouldBe AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.submit.url

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
      val field = ConfirmOtherRelevantIndividualsForm.hasOtherRelevantIndividuals
      val errorMessage = "Select yes if there are any other relevant individuals"
      val formWithError = ConfirmOtherRelevantIndividualsForm.form.withError(field, errorMessage)

      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = render(formWithError),
        heading = heading
      )
