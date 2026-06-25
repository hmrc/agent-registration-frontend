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

package uk.gov.hmrc.agentregistrationfrontend.views.individual.riskingoutcome

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingoutcome.OutcomeStartPage

class OutcomeStartPageSpec
extends ViewSpec:

  val viewTemplate: OutcomeStartPage = app.injector.instanceOf[OutcomeStartPage]
  private val linkId = LinkId("test-link-id")

  val doc: Document = Jsoup.parse(
    viewTemplate(
      linkId = linkId
    ).body
  )

  "OutcomeStartPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |Sign in to see the outcome of the application
           |Use sign in details for your personal taxes, not your business taxes.
           |Start
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "Sign in to see the outcome of the application - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Sign in to see the outcome of the application"

    "render a link to see the application outcome" in:
      val startLink: TestLink =
        doc
          .mainContent
          .selectOrFail(".govuk-button--start")
          .selectOnlyOneElementOrFail()
          .toLink

      startLink shouldBe TestLink(
        text = "Start",
        href = s"/agent-registration/provide-details/outcome/${linkId.value}"
      )
