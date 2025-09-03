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

package uk.gov.hmrc.agentregistrationfrontend.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.TimedOutPage

class TimedOutPageSpec
extends ViewSpec:

  "TimedOutPage" should:
    val viewTemplate: TimedOutPage = app.injector.instanceOf[TimedOutPage]
    val doc: Document = Jsoup.parse(viewTemplate().body)

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |You have been signed out
          |You have not done anything for 15 minutes, so we have signed you out to keep your account secure.
          |Sign in again
          |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "You have been signed out - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "You have been signed out"

    "render explanation for sign out" in:
      doc
        .mainContent
        .selectOrFail("p.govuk-body")
        .first()
        .text() shouldBe "You have not done anything for 15 minutes, so we have signed you out to keep your account secure."

    "render a link to sign in again" in:
      val signInAgainLink: TestLink =
        doc
          .mainContent
          .selectOrFail("p.govuk-body a")
          .selectOnlyOneElementOrFail()
          .toLink

      signInAgainLink shouldBe TestLink(
        text = "Sign in again",
        href = "/agent-registration"
      )
