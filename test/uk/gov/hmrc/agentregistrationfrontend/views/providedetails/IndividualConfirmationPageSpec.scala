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
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.individualconfirmation.IndividualConfirmationPage

class IndividualConfirmationPageSpec
extends ViewSpec:

  val viewTemplate: IndividualConfirmationPage = app.injector.instanceOf[IndividualConfirmationPage]
  val doc: Document = Jsoup.parse(
    viewTemplate(applicantName = "Test Applicant", companyName = "Test Company Name").body
  )

  "IndividualConfirmationPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |You have finished this process
          |What happens next
          |We need to collect information for all members of Test Company Name.
          |When we have the information we need, we’ll automatically submit the application and begin our checks.
          |If we need any further information from you, we will contact you by email or telephone.
          |When we have finished our checks, we’ll email Test Applicant with a decision about the application.
          |Finish and sign out
          |Is this page not working properly? (opens in new tab)
          |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "You have finished this process - Apply for an agent services account - GOV.UK"

    "have the correct h2" in:
      doc.h2 shouldBe "What happens next"
