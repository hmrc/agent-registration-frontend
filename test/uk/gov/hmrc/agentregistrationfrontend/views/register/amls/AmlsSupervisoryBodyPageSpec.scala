/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.views.register.amls

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.forms.SelectFromOptionsForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.amls.AmlsSupervisoryBodyPage

class AmlsSupervisoryBodyPageSpec
extends ViewSpec:

  val viewTemplate: AmlsSupervisoryBodyPage = app.injector.instanceOf[AmlsSupervisoryBodyPage]

  def testOptions: Map[String, String] = Map(
    "ATT" -> "Association of TaxationTechnicians (ATT)",
    "HMRC" -> "HM Revenue and Customs (HMRC)"
  )

  val doc: Document = Jsoup.parse(
    viewTemplate(
      // TODO: it won't confirm that the same for is used in controller when making a view from it. Also in controller that code seems to be duplicated
      SelectFromOptionsForm.form(
        fieldName = "amlsSupervisoryBody",
        options = testOptions.keys.toSeq
      )
    ).body
  )

  private val heading: String = "What is the name of your supervisory body?"

  "AmlsSupervisoryBodyPage view" should:

    "contain expected content" in:
      doc.mainContent shouldContainContent
        """
          |Anti-money laundering supervision details
          |What is the name of your supervisory body?
          |Start to enter a name and choose your supervisor from the list
          |Association of TaxationTechnicians (ATT)
          |HM Revenue and Customs (HMRC)
          |Save and continue
          |Save and come back later
          |""".stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "have a select element" in:
      doc.select("select").size() shouldBe 1

    "render a select element with Amls Supervisory Body options" in:
      val expectedElement = TestSelect(
        "amlsSupervisoryBody",
        Seq(("", "")) ++ testOptions.toSeq
      )
      doc
        .mainContent
        .selectOrFail("select")
        .selectOnlyOneElementOrFail()
        .toSelect shouldBe expectedElement

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

    "render an error message when form has errors" in:
      val field = "amlsSupervisoryBody"
      val errorMessage = "Enter a name and choose your supervisor from the list"
      // TODO: form is duplicated, not really testing that such form with this particular error will be used in controller
      val formWithError = SelectFromOptionsForm.form("amlsSupervisoryBody", testOptions.keys.toSeq)
        .withError(field, errorMessage)
      val errorDoc: Document = Jsoup.parse(viewTemplate(formWithError).body)

      errorDoc.mainContent shouldContainContent
        """
          |There is a problem
          |Enter a name and choose your supervisor from the list
          |Anti-money laundering supervision details
          |What is the name of your supervisory body?
          |Start to enter a name and choose your supervisor from the list
          |Error:
          |Enter a name and choose your supervisor from the list
          |Association of TaxationTechnicians (ATT)
          |HM Revenue and Customs (HMRC)
          |Save and continue
          |Save and come back later
          |""".stripMargin

      errorDoc.title() shouldBe s"Error: $heading - Apply for an agent services account - GOV.UK"
      errorDoc.selectOrFail(".govuk-error-summary__title").selectOnlyOneElementOrFail().text() shouldBe "There is a problem"
      errorDoc.selectOrFail(".govuk-error-summary__list > li > a").selectOnlyOneElementOrFail().selectAttrOrFail("href") shouldBe s"#$field"
      errorDoc.selectOrFail(".govuk-error-message").selectOnlyOneElementOrFail().text() shouldBe s"Error: $errorMessage"
