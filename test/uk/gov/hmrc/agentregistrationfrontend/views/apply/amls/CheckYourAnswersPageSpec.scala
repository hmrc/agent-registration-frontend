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

package uk.gov.hmrc.agentregistrationfrontend.views.apply.amls

import com.google.inject.AbstractModule
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.amls.CheckYourAnswersPage

class CheckYourAnswersPageSpec
extends ViewSpec:

  override lazy val overridesModule: AbstractModule =
    new AbstractModule:
      override def configure(): Unit = bind(classOf[AmlsCodes]).asEagerSingleton()

  val viewTemplate: CheckYourAnswersPage = app.injector.instanceOf[CheckYourAnswersPage]

  private object agentApplication:

    val completeHmrcApplication: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsHmrc
        .complete

    val completeNonHmrcApplication: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .complete

  private val heading: String = "Check your answers"

  "CheckYourAnswersPage for complete Hmrc Amls Details" should:
    implicit val agentApplicationHmrcRequest: AgentApplicationRequest[AnyContent] = tdAll.makeAgentApplicationRequest(agentApplication.completeHmrcApplication)

    val doc: Document = Jsoup.parse(viewTemplate().body)
    "contain content" in:
      doc.mainContent shouldContainContent
        """
          |Anti-money laundering supervision details
          |Check your answers
          |Supervisory body
          |HM Revenue and Customs (HMRC)
          |Change Supervisory body
          |Registration number
          |XAML00000123456
          |Change Registration number
          """.stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a summary row for each required answer" in:
      val expectedSummaryList: TestSummaryList = TestSummaryList(
        List(
          TestSummaryRow(
            key = "Supervisory body",
            value = "HM Revenue and Customs (HMRC)",
            action = "/agent-registration/apply/anti-money-laundering/supervisor-name"
          ),
          TestSummaryRow(
            key = "Registration number",
            value = "XAML00000123456",
            action = "/agent-registration/apply/anti-money-laundering/registration-number"
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a confirm and continue button" in:
      doc.extractLinkButton(1).text shouldBe "Confirm and continue"

  "CheckYourAnswersPage for complete non-Hmrc Amls Details" should:
    implicit val agentApplicationHmrcRequest: AgentApplicationRequest[AnyContent] = tdAll.makeAgentApplicationRequest(
      agentApplication.completeNonHmrcApplication
    )

    val doc: Document = Jsoup.parse(viewTemplate().body)
    "contain content" in:
      doc.mainContent shouldContainContent
        """
          |Anti-money laundering supervision details
          |Check your answers
          |Supervisory body
          |Financial Conduct Authority (FCA)
          |Change Supervisory body
          |Registration number
          |1234567890
          |Change Registration number
          |Supervision expiry date
          |2 September 2026
          |Change Supervision expiry date
          |Evidence of anti-money laundering supervision
          |test.pdf
          |Change Evidence of anti-money laundering supervision
          """.stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a summary row for each required answer" in:
      val expectedSummaryList: TestSummaryList = TestSummaryList(
        List(
          TestSummaryRow(
            key = "Supervisory body",
            value = "Financial Conduct Authority (FCA)",
            action = "/agent-registration/apply/anti-money-laundering/supervisor-name"
          ),
          TestSummaryRow(
            key = "Registration number",
            value = "1234567890",
            action = "/agent-registration/apply/anti-money-laundering/registration-number"
          ),
          TestSummaryRow(
            key = "Supervision expiry date",
            value = "2 September 2026",
            action = "/agent-registration/apply/anti-money-laundering/supervision-runs-out"
          ),
          TestSummaryRow(
            key = "Evidence of anti-money laundering supervision",
            value = "test.pdf",
            action = "/agent-registration/apply/anti-money-laundering/evidence"
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a confirm and continue button" in:
      doc.extractLinkButton(1).text shouldBe "Confirm and continue"
