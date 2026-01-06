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
import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.ConfirmationPage

class ConfirmationPageSpec
extends ViewSpec:

  val viewTemplate: ConfirmationPage = app.injector.instanceOf[ConfirmationPage]
  implicit val agentApplicationRequest: AgentApplicationRequest[AnyContent] = tdAll.makeAgentApplicationRequest(
    agentApplication =
      tdAll
        .agentApplicationLlp
        .afterDeclarationSubmitted
  )
  val doc: Document = Jsoup.parse(
    viewTemplate(entityName = "Test Company Name").body
  )

  "ConfirmationPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |You’ve finished the first stage of the application
           |Application reference: HDJ2123F
           |What to do next
           |Everyone listed in Companies House as a current member of Test Company Name needs to sign in and provide some personal details.
           |Send this link to all members:
           |$thisFrontendBaseUrl/agent-registration/provide-details/start/link-id-12345
           |Copy link to clipboard
           |Link copied
           |How to check the progress of your application
           |You can click the link on the GOV.UK page “Apply for an agent services account”.
           |This will let you check who has provided their personal details.
           |When everyone has provided their personal details, we will:
           |automatically submit your application
           |email you at user@test.com to confirm this
           |View or print your application
           |Finish and sign out
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "You’ve finished the first stage of the application - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "You’ve finished the first stage of the application"
