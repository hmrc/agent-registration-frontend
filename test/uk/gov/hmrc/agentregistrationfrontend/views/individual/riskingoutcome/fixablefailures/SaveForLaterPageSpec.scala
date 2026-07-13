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

package uk.gov.hmrc.agentregistrationfrontend.views.individual.riskingoutcome.fixablefailures

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.util.DisplayDate
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingoutcome.fixablefailures.SaveForLaterPage

import java.time.LocalDate
import java.time.ZoneId

class SaveForLaterPageSpec
extends ViewSpec:

  val viewTemplate: SaveForLaterPage = app.injector.instanceOf[SaveForLaterPage]
  val expiryDate: LocalDate = tdAll.applicationExpiresAtAsInstant.atZone(ZoneId.systemDefault()).toLocalDate
  val expiryDateDisplay: String = DisplayDate.displayDateForLang(Some(expiryDate))
  val doc: Document = Jsoup.parse(
    viewTemplate(expiryDateDisplay, tdAll.linkId).body
  )

  "SaveForLaterPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |Your progress will be saved until $expiryDateDisplay
           |What you need to do next
           |You need to come back and complete all the actions by $expiryDateDisplay.
           |To come back to this application:
           |Use the original link sent to you by the applicant
           |Sign in using your details for your personal taxes not your business taxes
           |If you no longer have the link, ask the applicant to send it to you again.
           |Continue with the application
           |Finish and sign out
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"Your progress will be saved until $expiryDateDisplay - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe s"Your progress will be saved until $expiryDateDisplay"

    "have the correct h2" in:
      doc.h2 shouldBe "What you need to do next"

    "render first paragraph" in:
      doc
        .mainContent
        .selectOrFail("p.govuk-body")
        .first()
        .text() shouldBe s"You need to come back and complete all the actions by $expiryDateDisplay."

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
            "Use the original link sent to you by the applicant",
            "Sign in using your details for your personal taxes not your business taxes"
          )
        ))

    "render a link to the task list" in:
      val taskListLink: TestLink =
        doc
          .mainContent
          .selectOrFail("a.govuk-link")
          .get(0)
          .toLink

      taskListLink shouldBe TestLink(
        text = "Continue with the application",
        href = "/agent-registration/provide-details/conditions-not-yet-met/task-list/link-id-12345"
      )

    "render a link to finish and sign out" in:
      val signOutLink: TestLink =
        doc
          .mainContent
          .selectOrFail("a.govuk-link")
          .get(1)
          .toLink

      signOutLink shouldBe TestLink(
        text = "Finish and sign out",
        href = "/agent-registration/sign-out-with-continue?continueUrl=https%3A%2F%2Fwww.gov.uk%2Fguidance%2Fget-an-hmrc-agent-services-account"
      )
