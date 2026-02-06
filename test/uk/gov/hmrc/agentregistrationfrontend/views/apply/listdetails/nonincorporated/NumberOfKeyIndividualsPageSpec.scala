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

package uk.gov.hmrc.agentregistrationfrontend.views.apply.listdetails.nonincorporated

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.forms.NumberOfKeyIndividualsForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.listdetails.nonincorporated.NumberOfKeyIndividualsPage

class NumberOfKeyIndividualsPageSpec
extends ViewSpec:

  val viewTemplate: NumberOfKeyIndividualsPage = app.injector.instanceOf[NumberOfKeyIndividualsPage]
  val agentApplication: AgentApplication =
    tdAll
      .agentApplicationGeneralPartnership
      .sectionAgentDetails
      .whenUsingExistingCompanyName
      .afterBusinessNameProvided
  val doc: Document = Jsoup.parse(viewTemplate(
    form = NumberOfKeyIndividualsForm.form,
    entityName = tdAll.companyName,
    agentApplication = agentApplication
  ).body)
  private val heading: String = "How many partners are there at Test Company Name?"

  "NumberOfKeyIndividualsPage" should:

    "contain expected content" in:
      doc.mainContent shouldContainContent
        """
          |Partner and tax adviser information
          |How many partners are there at Test Company Name?
          |Only tell us about people with the title ‘partner’. Do not include partner organisations.
          |5 or fewer
          |What is the exact number?
          |6 or more
          |How many are responsible for tax matters?
          |This means material responsibility for tax advice activities or significant authority over HMRC interactions.
          |Save and continue
          |Save and come back later
          |Is this page not working properly? (opens in new tab)
          |""".stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a radio button for each option" in:
      val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
        legend = heading,
        options = List(
          "5 or fewer" -> "FiveOrLess",
          "6 or more" -> "SixOrMore"
        ),
        hint = Some(
          "Only tell us about people with the title ‘partner’. Do not include partner organisations."
        )
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
      val field = NumberOfKeyIndividualsForm.howManyIndividualsOption
      val errorMessage = "Select how many partners there are"
      val formWithError = NumberOfKeyIndividualsForm.form
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(
          form = formWithError,
          entityName = tdAll.companyName,
          agentApplication = agentApplication
        ).body),
        heading = heading
      )
