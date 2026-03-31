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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.aboutyourbusiness

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.aboutyourbusiness.ConfirmDeleteAndStartAgainPage

class ConfirmDeleteAndStartAgainPageSpec
extends ViewSpec:

  val viewTemplate: ConfirmDeleteAndStartAgainPage = app.injector.instanceOf[ConfirmDeleteAndStartAgainPage]
  implicit val doc: Document = Jsoup.parse(viewTemplate().body)
  private val heading: String = "Confirm you want to delete your application and start again"

  "ConfirmDeleteAndStartAgainPage" should:

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a form with correct action" in:
      val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe AppRoutes.apply.aboutyourbusiness.ConfirmDeleteAndStartAgainController.submit.url

    "render a start again button to post the form" in:
      doc.select("form button[type=submit]").text() shouldBe "Start again"

    "render a cancel button that links back to the last page" in:
      doc.select("a.govuk-button--secondary").text() shouldBe "Cancel"
      doc.select("a.govuk-button--secondary").attr("href") shouldBe AppRoutes.apply.aboutyourbusiness.CheckYourAnswersController.show.url
