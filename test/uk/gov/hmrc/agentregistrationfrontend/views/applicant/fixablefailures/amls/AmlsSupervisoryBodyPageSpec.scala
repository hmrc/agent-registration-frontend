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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.fixablefailures.amls

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import uk.gov.hmrc.agentregistration.shared.amls.AmlsSupervisoryBodyCode
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsCodeForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.amls.AmlsSupervisoryBodyPage

class AmlsSupervisoryBodyPageSpec
extends ViewSpec:

  val viewTemplate: AmlsSupervisoryBodyPage = app.injector.instanceOf[AmlsSupervisoryBodyPage]

  val form: Form[AmlsSupervisoryBodyCode] = app.injector.instanceOf[AmlsCodeForm].form

  def testOptions: Map[String, String] = Map(
    "ATT" -> "Association of TaxationTechnicians (ATT)",
    "HMRC" -> "HM Revenue and Customs (HMRC)"
  )

  val doc: Document = Jsoup.parse(
    viewTemplate(form, "Test Company").body
  )

  private val heading: String = "What is the name of the supervisory body for Test Company?"

  "AmlsSupervisoryBodyPage view" should:

    "contain expected content" in:
      doc.mainContent shouldContainContent
        """
          |Anti-money laundering supervision details
          |What is the name of the supervisory body for Test Company?
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

    "render a form to submit confirmation of supervisory body selection" in:
      val form = doc.select("form")
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe AppRoutes.fixablefailures.amlsfailure.AmlsSupervisorController.submit.url
      form.selectOrFail(s"button[value='${SaveAndContinue.toString}']").selectOnlyOneElementOrFail()
      form.selectOrFail(s"a[href=${AppRoutes.fixablefailures.SaveForLaterController.show.url}]").selectOnlyOneElementOrFail()

    "render an error message when form has errors" in:
      val field = "amlsSupervisoryBody"
      // we are not testing if this is correct error message content,
      // we are testing how any given error is rendered in this template
      val errorMessage = "Enter a name and choose your supervisor from the list"
      val formWithError = form.withError(field, errorMessage)
      val errorDoc: Document = Jsoup.parse(viewTemplate(formWithError, "Test Company").body)
      errorDoc.mainContent shouldContainContent
        """
          |There is a problem
          |Enter a name and choose your supervisor from the list
          |Anti-money laundering supervision details
          |What is the name of the supervisory body for Test Company?
          |Start to enter a name and choose your supervisor from the list
          |Error:
          |Enter a name and choose your supervisor from the list
          |Association of TaxationTechnicians (ATT)
          |HM Revenue and Customs (HMRC)
          |Save and continue
          |Save and come back later
          |""".stripMargin

      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = errorDoc,
        heading = heading
      )
