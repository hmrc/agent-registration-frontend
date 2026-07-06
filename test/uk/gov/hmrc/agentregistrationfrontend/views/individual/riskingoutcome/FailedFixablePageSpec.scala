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

package uk.gov.hmrc.agentregistrationfrontend.views.individual.riskingoutcome

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingoutcome.FailedFixablePage

class FailedFixablePageSpec
extends ViewSpec:

  val viewTemplate: FailedFixablePage = app.injector.instanceOf[FailedFixablePage]

  val doc: Document = Jsoup.parse(
    viewTemplate(
      linkId = tdAll.linkId,
      entityName = "Test Company Name",
      correctiveActionExpiryDate = "3 August 2026",
      actualDecisionDate = "4 June 2026"
    ).body
  )

  "FailedFixablePage for individual" should:
    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |Application outcome
           |You do not meet the registration conditions yet
           |Date of decision: 4 June 2026
           |Our decision
           |The application by Test Company Name for an agent services account cannot currently be approved.
           |The application is refused under Section 230 (registration conditions) of the Finance Act 2026 (opens in a new tab).
           |This is because you, as a relevant individual to the application, have not met one or more conditions of registration.
           |However, you can still meet the registration conditions by taking action before 3 August 2026
           |How to meet the registration conditions
           |The actions to take are listed on the next page.
           |View actions to take
           |Failure to meet the registration conditions
           |If you choose not to take action to meet the registration conditions:
           |Test Company Name will not be given an agent services account on this occasion
           |your application will be deleted on 3 August 2026 to comply with our data retention policy
           |You have the right to review or appeal
           |If you disagree with our decision, you should speak to Test Company Name.
           |If Test Company Name disagrees with our decision to refuse their application, they can request a review or appeal the decision (opens in a new tab).
           |Is this page not working properly? (opens in new tab)
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "You do not meet the registration conditions yet - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "You do not meet the registration conditions yet"

    "should contain a link to the actions to take page" in:
      val actionsLink: TestLink =
        doc
          .mainContent
          .selectOrFail("a.govuk-button--start")
          .get(0)
          .toLink

      actionsLink shouldBe TestLink(
        text = "View actions to take",
        href = AppRoutes.providedetails.riskingoutcome.fixablefailures.FixableTaskListController.show(tdAll.linkId).url
      )

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
