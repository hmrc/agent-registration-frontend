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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.expired

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.expired.UnsubmittedApplicationPage

class UnsubmittedApplicationPageSpec
extends ViewSpec:

  val viewTemplate: UnsubmittedApplicationPage = app.injector.instanceOf[UnsubmittedApplicationPage]
  implicit val doc: Document = Jsoup.parse(viewTemplate(
    numberOfDaysGracePeriod = 45,
    numberOfDaysToSubmitApplication = 73
  ).body)
  private val heading: String = "Your application has expired"

  "UnsubmittedApplicationPage should have expected content" in:
    doc.mainContent shouldContainContent
      """
        |Your application has expired
        |You started your application 73 days ago or more and did not submit it.
        |You can delete this application and start a new one.
        |If you do not delete this application, we will delete it 45 days after it expired. This will remove all the information you entered.
        |Delete and start new application
        |Finish and sign out
        |"""
        .stripMargin

  "UnsubmittedApplicationPage should have the correct title" in:
    doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

  "UnsubmittedApplicationPage should render a form with correct action" in:
    val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
    form.attr("method") shouldBe "POST"
    form.attr("action") shouldBe AppRoutes.apply.expired.UnsubmittedApplicationController.submit.url

  "UnsubmittedApplicationPage should render a button to post the form" in:
    doc.select("form button[type=submit]").text() shouldBe "Delete and start new application"

  "UnsubmittedApplicationPage should render a link to finish and sign out" in:
    behavesLikePageWithFinishedAndSignOutLink(doc)
