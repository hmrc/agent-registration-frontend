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

package uk.gov.hmrc.agentregistrationfrontend.views.individual

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingprogress.IndividualConfirmationPage

class IndividualConfirmationPageSpec
extends ViewSpec:

  val viewTemplate: IndividualConfirmationPage = app.injector.instanceOf[IndividualConfirmationPage]
  val doc: Document = Jsoup.parse(
    viewTemplate(
      applicantName = "Test Applicant",
      entityName = "Test Company",
      isSoleTraderOwner = false
    ).body
  )

  val soleTraderDoc: Document = Jsoup.parse(
    viewTemplate(
      applicantName = "ST Name ST Surname",
      entityName = "ST Name ST Surname",
      isSoleTraderOwner = true
    ).body
  )

  "IndividualConfirmationPage" should:

    "have expected content when not sole trader" in:
      doc.mainContent shouldContainContent
        """
          |You have finished this process
          |What happens next
          |Your information has been added to Test Company’s application for an agent services account. Once the application has been submitted we’ll begin our checks.
          |When we have finished our checks, we’ll email Test Applicant with a decision about the application.
          |If we need any further information from you, we will contact you by email or telephone.
          |Finish and sign out
          |Is this page not working properly? (opens in new tab)
          |"""
          .stripMargin

    "have the correct title when not sole trader" in:
      doc.title() shouldBe "You have finished this process - Apply for an agent services account - GOV.UK"

    "have the correct h2 when not sole trader" in:
      doc.h2 shouldBe "What happens next"

    "have expected content when sole trader" in:
      soleTraderDoc.mainContent shouldContainContent
        """
          |You have proved your identity
          |You need to sign back in with your agent sign in details to continue your application.
          |Continue your application using your agent sign in details
          |Is this page not working properly? (opens in new tab)
          |"""
          .stripMargin

    "have the correct title when sole trader" in:
      soleTraderDoc.title() shouldBe "You have proved your identity - Apply for an agent services account - GOV.UK"
