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

package uk.gov.hmrc.agentregistrationfrontend.views.register

import com.softwaremill.quicklens.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessType.SoleTrader
import uk.gov.hmrc.agentregistration.shared.UserRole.Owner
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationFactory
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpecSupport
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.CheckYourAnswerPage

class CheckYourAnswerPageSpec
extends ViewSpecSupport {

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
  implicit val doc: Document = Jsoup.parse(viewTemplate().body)
  private val heading: String = "Check your answers"

  "CheckYourAnswerPage" should {

    "have the correct title" in {
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"
    }

    "render a radio button for each option" in {
      val testSummaryList: TestSummaryList = TestSummaryList(
        List(
          ("Business type", "Sole trader", "/agent-registration/register/about-your-application/business-type"),
          ("Are you the business owner?", "Yes", "/agent-registration/register/about-your-application/user-role")
        )
      )
      doc.mainContent.extractSummaryList(1).value shouldBe testSummaryList
    }

    "render a confirm and continue button" in {
      doc.select("button[type=submit]").text() shouldBe "Confirm and continue"
    }

  }

}
