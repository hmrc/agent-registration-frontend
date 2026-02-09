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
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.SaveForLaterPage

class SaveForLaterPageSpec
extends ViewSpec:

  val viewTemplate: SaveForLaterPage = app.injector.instanceOf[SaveForLaterPage]

  val doc: Document = Jsoup.parse(
    viewTemplate().body
  )

  "SaveForLaterPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |Your progress has been saved for 30 days
          |What you need to do next
          |You need to come back and complete this application within 30 days.
          |To come back to this application:
          |Go to the Apply for an agent services account page.
          |Click on the link under the start button to check the progress of your application.
          |Sign in again using the same credentials you are currently signed in with.
          |Continue where you left off
          |Finish and sign out
          |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "Your progress has been saved for 30 days - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Your progress has been saved for 30 days"

    "have the correct h2" in:
      doc.h2 shouldBe "What you need to do next"

    "render first paragraph" in:
      doc
        .mainContent
        .selectOrFail("p.govuk-body")
        .first()
        .text() shouldBe "You need to come back and complete this application within 30 days."

    "render second paragraph" in:
      doc
        .mainContent
        .selectOrFail("p.govuk-body")
        .get(1)
        .text() shouldBe "To come back to this application:"

    "render numbered steps" in:
      doc
        .mainContent
        .extractNumberedList().shouldBe(TestNumberedList(
          items = List(
            "Go to the Apply for an agent services account page.",
            "Click on the link under the start button to check the progress of your application.",
            "Sign in again using the same credentials you are currently signed in with."
          )
        ))

    "render a link to the Govuk start page" in:
      val startPageLink: TestLink =
        doc
          .mainContent
          .selectOrFail("li a.govuk-link")
          .selectOnlyOneElementOrFail()
          .toLink

      startPageLink shouldBe TestLink(
        text = "Apply for an agent services account",
        href = "https://www.gov.uk/guidance/get-an-hmrc-agent-services-account"
      )

    "render a link to the task list" in:
      val taskListLink: TestLink =
        doc
          .mainContent
          .selectOrFail("a.govuk-link")
          .get(1)
          .toLink

      taskListLink shouldBe TestLink(
        text = "Continue where you left off",
        href = "/agent-registration/apply/task-list"
      )

    "render a link to finish and sign out" in:
      val signOutLink: TestLink =
        doc
          .mainContent
          .selectOrFail("a.govuk-link")
          .get(2)
          .toLink

      signOutLink shouldBe TestLink(
        text = "Finish and sign out",
        href = "/agent-registration/sign-out"
      )
