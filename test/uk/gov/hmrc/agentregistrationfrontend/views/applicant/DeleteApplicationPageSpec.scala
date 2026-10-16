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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import uk.gov.hmrc.agentregistrationfrontend.forms.ConfirmDeleteApplicationForm
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.DeleteApplicationPage

class DeleteApplicationPageSpec
extends ViewSpec:

  val viewTemplate: DeleteApplicationPage = app.injector.instanceOf[DeleteApplicationPage]
  implicit val doc: Document = Jsoup.parse(viewTemplate(ConfirmDeleteApplicationForm.form).body)
  private val heading: String = "Delete your application"

  "DeleteApplicationPage should have expected content" in:
    doc.mainContent shouldContainContent
      """
        |Delete your application
        |This action will happen immediately. You will no longer be able to access any details about your current application, including our decision.
        |You can choose to start a new application when you are ready.
        |HMRC carries out checks against all applications and any relevant individuals.
        |Do you want to delete your application?
        |Yes
        |No
        |Continue
        |"""
        .stripMargin

  "DeleteApplicationPage should have the correct title" in:
    doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

  "DeleteApplicationPage should render a form with correct action" in:
    val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
    form.attr("method") shouldBe "POST"
    form.attr("action") shouldBe AppRoutes.apply.DeleteApplicationController.submit.url

  "DeleteApplicationPage should render a radio button for each option" in:
    val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
      legend = "Do you want to delete your application?",
      options = List(
        "Yes" -> YesNo.Yes.toString,
        "No" -> YesNo.No.toString
      ),
      hint = None
    )
    doc.mainContent.extractRadioGroup() shouldBe expectedRadioGroup

  "DeleteApplicationPage should render a button to post the form" in:
    doc.select("form button[type=submit]").text() shouldBe "Continue"

  "DeleteApplicationPage should render the form error correctly when the form contains an error" in:
    val errorMessage: String = "Select yes if you want to delete your application"
    val formWithError: Form[YesNo] = ConfirmDeleteApplicationForm.form
      .bind(Map.empty[String, String])
    behavesLikePageWithErrorHandling(
      field = ConfirmDeleteApplicationForm.key,
      errorMessage = errorMessage,
      errorDoc = Jsoup.parse(viewTemplate(
        form = formWithError
      ).body),
      heading = heading
    )
