/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.FailedFixableStartPage

class FailedFixableStartPageSpec
extends ViewSpec:

  val viewTemplate: FailedFixableStartPage = app.injector.instanceOf[FailedFixableStartPage]

  val doc: Document = Jsoup.parse(
    viewTemplate(
      actualDecisionDate = "4 June 2026",
      correctiveActionExpiryDate = "3 August 2026",
      entityName = "Test Company Name"
    ).body
  )

  "FailedNonFixablePage when individuals have failures" should:
    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |Application outcome
           |Test Company Name does not meet the registration conditions
           |Date of decision: 4 June 2026
           |Our decision
           |Your application to register for an agent services account cannot currently be approved.
           |The application is refused under Section 230 (registration conditions) of the Finance Act 2026 (opens in a new tab).
           |However, Test Company Name can still meet the registration conditions if you take action by 3 August 2026.
           |How to meet the registration conditions
           |The actions to take are listed on the next page.
           |View actions to take
           |Failure to meet the registration conditions
           |If you choose not to take action to meet the registration conditions:
           |Test Company Name will not be given an agent services account on this occasion
           |the application will be deleted on 3 August 2026 to comply with our data retention policy
           |Test Company Name has the right to review or appeal
           |Keep a copy of this decision for your records.
           |If you disagree with our decision to refuse your application, you can request a review or appeal the decision (opens in a new tab).
           |You can ask for a review of our decision or begin the appeal process even if you decide to take the actions we need.
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "Test Company Name does not meet the registration conditions - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Test Company Name does not meet the registration conditions"

    "should contain a link to the appeals guidance" in:
      val appealsLink: TestLink =
        doc
          .mainContent
          .selectOrFail("a.govuk-link")
          .get(1)
          .toLink

      appealsLink shouldBe TestLink(
        text = "request a review or appeal the decision (opens in a new tab)",
        href = "https://www.gov.uk/guidance/if-you-disagree-with-hmrcs-decision-about-your-tax-adviser-registration"
      )
