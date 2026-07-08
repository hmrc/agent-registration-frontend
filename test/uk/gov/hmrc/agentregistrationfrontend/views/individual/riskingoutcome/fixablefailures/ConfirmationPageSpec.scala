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

package uk.gov.hmrc.agentregistrationfrontend.views.individual.riskingoutcome.fixablefailures

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingoutcome.fixablefailures.ConfirmationPage

class ConfirmationPageSpec
extends ViewSpec:

  val viewTemplate: ConfirmationPage = app.injector.instanceOf[ConfirmationPage]

  val doc: Document = Jsoup.parse(
    viewTemplate(
      applicantName = tdAll.applicantName.value,
      individualEmailAddress = tdAll.individualVerifiedEmailAddress.emailAddress
    ).body
  )

  "Confirmation page for fixable failures for individuals" should:
    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |You have finished this process
           |What happens next
           |We’ll ask Alice Smith to resubmit the application for an agent services account, when everyone has provided the information we need.
           |We’ll send an email to you at member@test.com if we need anything else from you.
           |Finish and sign out
           |Is this page not working properly? (opens in new tab)
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "You have finished this process - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "You have finished this process"
