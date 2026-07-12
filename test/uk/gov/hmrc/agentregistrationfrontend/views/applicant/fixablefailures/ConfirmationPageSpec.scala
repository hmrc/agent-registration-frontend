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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.fixablefailures

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.ConfirmationPage

class ConfirmationPageSpec
extends ViewSpec:

  val viewTemplate: ConfirmationPage = app.injector.instanceOf[ConfirmationPage]
  val agentApplication: AgentApplicationLlp =
    tdAll
      .agentApplicationLlp
      .afterResubmitted

  val doc: Document = Jsoup.parse(
    viewTemplate(
      dateOfDecision = "1 September 2026",
      agentApplication = agentApplication
    ).body
  )

  "ConfirmationPage for fixable failures" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |You have resubmitted your application for an agent services account
           |Application reference: ${agentApplication.applicationReference.value}
           |What happens next
           |We’ll send you an email to confirm your application has been submitted. This email will tell you how to check the progress of your application.
           |View or print your application
           |Our decision
           |We’ll carry out some checks based on the information you’ve given us. If we need any more information we’ll contact you.
           |We aim to make a decision by 1 September 2026.
           |We’ll send an email to user@test.com to let you know the outcome.
           |Finish and sign out
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "You have resubmitted your application for an agent services account - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "You have resubmitted your application for an agent services account"
