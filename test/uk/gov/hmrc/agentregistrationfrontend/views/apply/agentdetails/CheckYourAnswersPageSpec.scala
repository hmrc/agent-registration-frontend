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

package uk.gov.hmrc.agentregistrationfrontend.views.apply.agentdetails

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.agentdetails.CheckYourAnswersPage

class CheckYourAnswersPageSpec
extends ViewSpec:

  val viewTemplate: CheckYourAnswersPage = app.injector.instanceOf[CheckYourAnswersPage]

  private val tdAll: TdAll = TdAll()

  object agentApplication:
    val complete: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterBprAddressSelected

  private val heading: String = "Check your answers"

  "CheckYourAnswersPage for complete Agent Details" should:
    given agentApplicationHmrcRequest: AgentApplicationRequest[AnyContent] = tdAll.makeAgentApplicationRequest(agentApplication.complete)

    val doc: Document = Jsoup.parse(viewTemplate().body)
    "contain content" in:
      doc.mainContent shouldContainContent
        """
          |Agent services account details
          |Check your answers
          |Name shown to clients
          |Test Company Name
          |Change Name shown to clients
          |Telephone number
          |(+44) 10794554342
          |Change Telephone number
          |Email address
          |new@example.com
          |Change Email address
          |Correspondence address
          |Registered Line 1
          |Registered Line 2
          |AB1 2CD
          |GB
          |Change Correspondence address
          |Confirm and continue
          """.stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a summary row for each required answer" in:
      val expectedSummaryList: TestSummaryList = TestSummaryList(
        List(
          TestSummaryRow(
            key = "Name shown to clients",
            value = "Test Company Name",
            action = AppRoutes.apply.agentdetails.AgentBusinessNameController.show.url,
            changeLinkAccessibleContent = "Change Name shown to clients"
          ),
          TestSummaryRow(
            key = "Telephone number",
            value = "(+44) 10794554342",
            action = AppRoutes.apply.agentdetails.AgentTelephoneNumberController.show.url,
            changeLinkAccessibleContent = "Change Telephone number"
          ),
          TestSummaryRow(
            key = "Email address",
            value = "new@example.com",
            action = AppRoutes.apply.agentdetails.AgentEmailAddressController.show.url,
            changeLinkAccessibleContent = "Change Email address"
          ),
          TestSummaryRow(
            key = "Correspondence address",
            value = "Registered Line 1 Registered Line 2 AB1 2CD GB",
            action = AppRoutes.apply.agentdetails.AgentCorrespondenceAddressController.show.url,
            changeLinkAccessibleContent = "Change Correspondence address"
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a confirm and continue button" in:
      doc.extractLinkButton(1).text shouldBe "Confirm and continue"
