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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.entitycheckfailed

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.checkfailed.CanNotRegisterPage

class CanNotRegisterPageSpec
extends ViewSpec:

  val viewTemplate: CanNotRegisterPage = app.injector.instanceOf[CanNotRegisterPage]

  val doc: Document = Jsoup.parse(
    viewTemplate("TestCompanyName").body
  )

  "CannotRegister" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |This TestCompanyName cannot be registered for an Agent Services Account
          |If you wish to register a different Agent, use this link (opens in new tab).
          |Try again
          |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "This TestCompanyName cannot be registered for an Agent Services Account - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "This TestCompanyName cannot be registered for an Agent Services Account"

    "render a try again button" in:
      val button = doc
        .mainContent
        .selectOrFail("a.govuk-button")
        .selectOnlyOneElementOrFail()

      button.text() shouldBe "Try again"
      button.attr("href") shouldBe "/agent-registration/apply/internal/refusal-to-deal-with-check"
