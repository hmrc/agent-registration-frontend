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
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.ConfirmationPage

class ConfirmationPageSpec
extends ViewSpec:

  val viewTemplate: ConfirmationPage = app.injector.instanceOf[ConfirmationPage]
  val agentApplication: AgentApplicationLlp =
    tdAll
      .agentApplicationLlp
      .afterDeclarationSubmitted

  val doc: Document = Jsoup.parse(
    viewTemplate(
      dateOfDecision = "21 April 2026",
      agentApplication = agentApplication
    ).body
  )

  "ConfirmationPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |You’ve applied for an agent services account
           |Application reference: ${agentApplication.agentApplicationId.value}
           |What happens next
           |We’ll carry out some checks based on the information you’ve given us.
           |If we need any more information we’ll contact you.
           |We aim to make a decision by 21 April 2026.
           |Our decision
           |We’ll send an email to let you know the outcome.
           |How to check the progress of an application
           |You can click the link on the GOV.UK page “Apply for an agent services account”.
           |View or print your application
           |Finish and sign out
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "You’ve applied for an agent services account - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "You’ve applied for an agent services account"
