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

package uk.gov.hmrc.agentregistrationfrontend.views.apply.agentdetails

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions.DataWithApplication
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentBusinessNameForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.agentdetails.AgentBusinessNamePage

class AgentBusinessNamePageSpec
extends ViewSpec:

  val viewTemplate: AgentBusinessNamePage = app.injector.instanceOf[AgentBusinessNamePage]
  implicit val agentApplicationRequest: RequestWithData[DataWithApplication] = tdAll.makeAgentApplicationRequest(
    agentApplication =
      tdAll
        .agentApplicationLlp
        .afterContactDetailsComplete
  )
  val doc: Document = Jsoup.parse(viewTemplate(
    form = AgentBusinessNameForm.form,
    bprBusinessName = Some("Test Company Name")
  ).body)
  private val heading: String = "What business name will you use for clients?"

  "AgentBusinessNamePage" should:

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a radio button for each option" in:
      val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
        legend = heading,
        options = List(
          "Test Company Name" -> "Test Company Name",
          "Something else" -> "other"
        ),
        hint = Some("Choose a name your clients know. We’ll use it when clients respond to authorisation requests or manage their agent relationships.")
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
      val field = AgentBusinessNameForm.key
      val errorMessage = "Enter the business name you want to use on your agent services account"
      val formWithError = AgentBusinessNameForm.form
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(formWithError, Some("Test Company Name")).body),
        heading = heading
      )
