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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.aboutyourbusiness

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions.DataWithApplication
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationfrontend.testsupport.viewspecsupport.ViewSelectors.*
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.aboutyourbusiness.CheckYourAnswersPage

class CheckYourAnswersPageSpec
extends ViewSpec:

  val viewTemplate: CheckYourAnswersPage = app.injector.instanceOf[CheckYourAnswersPage]

  private val tdAll: TdAll = TdAll()

  object agentApplication:
    val complete: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterGrsDataReceived

  private val heading: String = "Your business details"

  "CheckYourAnswersPage for complete GRS details received" should:
    given agentApplicationHmrcRequest: RequestWithData[DataWithApplication] = tdAll.makeAgentApplicationRequest(agentApplication.complete)
    val bpr: BusinessPartnerRecordResponse = tdAll.businessPartnerRecordResponse
    val doc: Document = Jsoup.parse(viewTemplate(bpr, agentApplicationHmrcRequest.agentApplication).body)
    "contain content" in:
      doc.mainContent shouldContainContent
        """
          |About your business
          |Your business details
          |The details you provided match your HMRC record.
          |If you change any of these details you will need to start the application again.
          |Business details
          |Change (Business details)
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
          |Return to task list
          |Save and come back later
          """.stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a read-only summary row for each required answer" in:
      val expectedSummaryList: TestReadOnlySummaryList = TestReadOnlySummaryList(
        List(
          TestReadOnlySummaryRow(
            key = "UK-based agent",
            value = "Yes"
          ),
          TestReadOnlySummaryRow(
            key = "Business type",
            value = "Limited liability partnership"
          ),
          TestReadOnlySummaryRow(
            key = "Are you a member of the limited liability partnership?",
            value = "No, but I’m authorised by them to set up this account"
          ),
          TestReadOnlySummaryRow(
            key = "Company name",
            value = "Test Company Name"
          ),
          TestReadOnlySummaryRow(
            key = "Unique taxpayer reference",
            value = "1234567895"
          )
        )
      )
      doc.mainContent.extractReadOnlySummaryList() shouldBe expectedSummaryList

    "render a change link for the entire business details section" in:
      doc.select(".govuk-summary-card__actions > a").text() shouldBe "Change (Business details)"
      doc.select(".govuk-summary-card__actions > a").attr("href") shouldBe AppRoutes.apply.aboutyourbusiness.ConfirmDeleteAndStartAgainController.show.url

    "render a button to return to task list" in:
      doc.extractLinkButton(1).text shouldBe "Return to task list"

    "render a button to save for later" in:
      doc.extractLinkButton(2).text shouldBe "Save and come back later"
