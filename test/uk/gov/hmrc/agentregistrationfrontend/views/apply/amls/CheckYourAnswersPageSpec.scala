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
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.DataWithApplication
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.RequestWithData
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.agentApplication
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
    implicit val agentApplicationHmrcRequest: RequestWithData[DataWithApplication] = tdAll.makeAgentApplicationRequest(
      agentApplication.completeHmrcApplication
    )

    val doc: Document = Jsoup.parse(viewTemplate(agentApplicationHmrcRequest.agentApplication).body)
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
            action = AppRoutes.apply.amls.AmlsSupervisorController.show.url,
            changeLinkAccessibleContent = "Change Supervisory body"
          ),
          TestSummaryRow(
            key = "Registration number",
            value = "XAML00000123456",
            action = AppRoutes.apply.amls.AmlsRegistrationNumberController.show.url,
            changeLinkAccessibleContent = "Change Registration number"
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a confirm and continue button" in:
      doc.extractLinkButton(1).text shouldBe "Confirm and continue"

  "CheckYourAnswersPage for complete non-Hmrc Amls Details" should:
    implicit val agentApplicationHmrcRequest: RequestWithData[DataWithApplication] = tdAll.makeAgentApplicationRequest(
      agentApplication.completeNonHmrcApplication
    )

    val doc: Document = Jsoup.parse(viewTemplate(agentApplicationHmrcRequest.agentApplication).body)
    "contain content" in:
      doc.mainContent shouldContainContent
        """
          |Anti-money laundering supervision details
          |Check your answers
          |Supervisory body
          |Association of TaxationTechnicians (ATT)
          |Change Supervisory body
          |Registration number
          |NONHMRC-REF-AMLS-NUMBER-00001
          |Change Registration number
          |Supervision expiry date
          |25 May 2060
          |Change Supervision expiry date
          |Evidence of anti-money laundering supervision
          |evidence.pdf
          |Change Evidence of anti-money laundering supervision
          """.stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a summary row for each required answer" in:
      val expectedSummaryList: TestSummaryList = TestSummaryList(
        List(
          TestSummaryRow(
            key = "Supervisory body",
            value = "Association of TaxationTechnicians (ATT)",
            action = AppRoutes.apply.amls.AmlsSupervisorController.show.url,
            changeLinkAccessibleContent = "Change Supervisory body"
          ),
          TestSummaryRow(
            key = "Registration number",
            value = "NONHMRC-REF-AMLS-NUMBER-00001",
            action = AppRoutes.apply.amls.AmlsRegistrationNumberController.show.url,
            changeLinkAccessibleContent = "Change Registration number"
          ),
          TestSummaryRow(
            key = "Supervision expiry date",
            value = "25 May 2060",
            action = AppRoutes.apply.amls.AmlsExpiryDateController.show.url,
            changeLinkAccessibleContent = "Change Supervision expiry date"
          ),
          TestSummaryRow(
            key = "Evidence of anti-money laundering supervision",
            value = "evidence.pdf",
            action = AppRoutes.apply.amls.AmlsEvidenceUploadController.showAmlsEvidenceUploadPage.url,
            changeLinkAccessibleContent = "Change Evidence of anti-money laundering supervision"
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a confirm and continue button" in:
      doc.extractLinkButton(1).text shouldBe "Confirm and continue"
