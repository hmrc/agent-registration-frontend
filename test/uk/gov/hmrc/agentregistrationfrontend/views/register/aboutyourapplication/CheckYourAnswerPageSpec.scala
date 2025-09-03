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

package uk.gov.hmrc.agentregistrationfrontend.views.register.aboutyourapplication

import com.softwaremill.quicklens.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessType.SoleTrader
import uk.gov.hmrc.agentregistration.shared.UserRole.Owner
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationFactory
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.aboutyourapplication.CheckYourAnswerPage

class CheckYourAnswerPageSpec
extends ViewSpec:

  val viewTemplate: CheckYourAnswerPage = app.injector.instanceOf[CheckYourAnswerPage]

  private val tdAll: TdAll = TdAll()
  private val applicationFactory = app.injector.instanceOf[ApplicationFactory]

  implicit val fakeAgentApplication: AgentApplication = applicationFactory
    .makeNewAgentApplication(tdAll.internalUserId)
    .modify(_.aboutYourApplication.businessType).setTo(Some(SoleTrader))
    .modify(_.aboutYourApplication.userRole).setTo(Some(Owner))

  implicit val agentApplicationRequest: AgentApplicationRequest[AnyContent] =
    new AgentApplicationRequest(
      request = request,
      agentApplication = fakeAgentApplication,
      internalUserId = tdAll.internalUserId,
      groupId = tdAll.groupId
    )

  val doc: Document = Jsoup.parse(viewTemplate().body)
  private val heading: String = "Check your answers"

  "CheckYourAnswerPage" should:
    "contain content" in:
      doc.mainContent shouldContainContent
        """
          |About your application
          |Check your answers
          |Business type
          |Sole trader
          |Change Business type
          |Are you the business owner?
          |Yes
          |Change Are you the business owner?
          """.stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a radio button for each option" in:
      val expectedSummaryList: TestSummaryList = TestSummaryList(
        List(
          TestSummaryRow(
            key = "Business type",
            value = "Sole trader",
            action = "/agent-registration/register/about-your-application/business-type"
          ),
          TestSummaryRow(
            key = "Are you the business owner?",
            value = "Yes",
            action = "/agent-registration/register/about-your-application/user-role"
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a confirm and continue button" in:
      doc.extractSubmitButtonText shouldBe "Confirm and continue"
