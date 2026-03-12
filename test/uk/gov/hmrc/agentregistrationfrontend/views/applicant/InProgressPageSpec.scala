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
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.InProgressPage

class InProgressPageSpec
extends ViewSpec:

  val viewTemplate: InProgressPage = app.injector.instanceOf[InProgressPage]
  val agentApplication: AgentApplicationLlp =
    tdAll
      .agentApplicationLlp
      .afterDeclarationSubmitted

  val doc: Document = Jsoup.parse(
    viewTemplate(
      entityName = "Test Company Name",
      agentApplication = agentApplication,
      dateOfDecision = "21 April 2026",
      dateSubmitted = "10 March 2026"
    ).body
  )

  "InProgressPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |Check the progress of an application
           |Application reference: ${agentApplication.agentApplicationId.value}
           |This is application is for an agent services account for Test Company Name.
           |Some of the information on this page uses real-time data and might change.
           |When to expect a decision
           |It's currently taking up to 8 weeks to process applications for an agent services account.
           |If we need further information to help us make a decision, we will contact you.
           |We expect to reach a decision by 21 April 2026 and will not be able to give you an update before this date.
           |We'll send you an email as soon as we've reached a decision.
           |Date submitted
           |10 March 2026
           |View or print your application (opens in a new tab)
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"Application reference: ${agentApplication.agentApplicationId.value} - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe s"Application reference: ${agentApplication.agentApplicationId.value}"
