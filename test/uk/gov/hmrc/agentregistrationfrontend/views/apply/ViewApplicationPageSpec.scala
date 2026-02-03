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

package uk.gov.hmrc.agentregistrationfrontend.views.apply

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.DataWithApplication
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.RequestWithData
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.agentApplication
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.ViewApplicationPage

class ViewApplicationPageSpec
extends ViewSpec:

  val viewTemplate: ViewApplicationPage = app.injector.instanceOf[ViewApplicationPage]
  implicit val agentApplicationRequest: RequestWithData[DataWithApplication] = tdAll.makeAgentApplicationRequest(
    agentApplication =
      tdAll
        .agentApplicationLlp
        .afterDeclarationSubmitted
  )
  val doc: Document = Jsoup.parse(
    viewTemplate(
      entityName = "Test Company Name",
      agentApplication = agentApplicationRequest.agentApplication
    ).body
  )

  "ViewApplicationPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |Application for Test Company Name
           |Application reference: HDJ2123F
           |About your business
           |UK-based agent
           |Yes
           |Business type
           |Limited liability partnership
           |Are you a member of the limited liability partnership?
           |No, but I’m authorised by them to set up this account
           |Company name
           |Test Company Name
           |Unique taxpayer reference
           |1234567895
           |Applicant contact details
           |Name
           |Alice Smith
           |Telephone number
           |(+44) 10794554342
           |Email address
           |user@test.com
           |Agency contact details
           |Name shown to clients
           |Test Company Name
           |Telephone number
           |(+44) 10794554342
           |Email address
           |user@test.com
           |Correspondence address
           |23 Great Portland Street
           |London
           |W1 8LT
           |GB
           |Anti-money laundering supervision details
           |Supervisory body
           |HM Revenue and Customs (HMRC)
           |Registration number
           |XAML1234567890
           |HMRC standard for agents
           |Agreed to meet the HMRC standard for agents
           |Yes
           |Print this page
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "Application for Test Company Name - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Application for Test Company Name"
