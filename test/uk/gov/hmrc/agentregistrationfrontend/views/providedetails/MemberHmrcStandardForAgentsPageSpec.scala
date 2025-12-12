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

package uk.gov.hmrc.agentregistrationfrontend.views.providedetails

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.memberconfirmation.MemberHmrcStandardForAgentsPage

class MemberHmrcStandardForAgentsPageSpec
extends ViewSpec:

  val viewTemplate: MemberHmrcStandardForAgentsPage = app.injector.instanceOf[MemberHmrcStandardForAgentsPage]

  val doc: Document = Jsoup.parse(
    viewTemplate().body
  )

  "HmrcStandardForAgentsPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |HMRC standard for agents
          |Agree to meet the HMRC standard for agents
          |!
          |Warning
          |We carry out checks to make sure agents meet the standard. We take action against individuals and organisations who do not comply.
          |Read the standard on GOV.UK
          |The HMRC standard for agents (opens in new tab)
          |Declaration
          |I have read the HMRC standard for agents.
          |I agree to meet the standard when working on behalf of clients.
          |Agree and continue
          |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "Agree to meet the HMRC standard for agents - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Agree to meet the HMRC standard for agents"

    "render a link to the Govuk page for the HMRC standard for agents" in:
      val hmrcStandardLink: TestLink =
        doc
          .mainContent
          .selectOrFail("a.govuk-link")
          .get(0)
          .toLink

      hmrcStandardLink shouldBe TestLink(
        text = "The HMRC standard for agents (opens in new tab)",
        href = "https://www.gov.uk/government/publications/hmrc-the-standard-for-agents"
      )

    "render an agree and continue button" in:
      doc
        .mainContent
        .selectOrFail(s"form button[value='AgreeAndContinue']")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Agree and continue"
