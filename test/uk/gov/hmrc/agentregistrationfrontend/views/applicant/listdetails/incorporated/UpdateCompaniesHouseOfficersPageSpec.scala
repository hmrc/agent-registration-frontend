/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.incorporated

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.UpdateCompaniesHouseOfficersPage

class UpdateCompaniesHouseOfficersPageSpec
extends ViewSpec:

  val viewTemplate: UpdateCompaniesHouseOfficersPage = app.injector.instanceOf[UpdateCompaniesHouseOfficersPage]

  private val entityName: String = tdAll.companyName
  private val heading: String = "You need to update Companies House"

  case class BusinessTypeTestCase(
    label: String,
    agentApplication: AgentApplication,
    caption: String,
    p1EntityType: String
  )

  private val testCases = Seq(
    BusinessTypeTestCase(
      label = "LimitedLiabilityPartnership",
      agentApplication = tdAll.agentApplicationLlp.afterHmrcStandardForAgentsAgreed,
      caption = "LLP members and other relevant individuals",
      p1EntityType = "members"
    ),
    BusinessTypeTestCase(
      label = "LimitedCompany",
      agentApplication = tdAll.agentApplicationLimitedCompany.afterHmrcStandardForAgentsAgreed,
      caption = "Directors and other relevant individuals",
      p1EntityType = "directors"
    ),
    BusinessTypeTestCase(
      label = "LimitedPartnership",
      agentApplication = tdAll.agentApplicationLimitedPartnership.afterHmrcStandardForAgentsAgreed,
      caption = "Partners and other relevant individuals",
      p1EntityType = "partners"
    ),
    BusinessTypeTestCase(
      label = "ScottishLimitedPartnership",
      agentApplication = tdAll.agentApplicationScottishLimitedPartnership.afterHmrcStandardForAgentsAgreed,
      caption = "Partners and other relevant individuals",
      p1EntityType = "partners"
    )
  )

  private def render(agentApplication: AgentApplication): Document = Jsoup.parse(viewTemplate(
    entityName = entityName,
    agentApplication = agentApplication
  ).body)

  for testCase <- testCases do
    s"UpdateCompaniesHouseOfficersPage for ${testCase.label}" should:

      val doc: Document = render(testCase.agentApplication)

      "have the correct caption" in:
        doc.mainContent.select("h2.govuk-caption-l").text() shouldBe testCase.caption

      "have the correct heading" in:
        doc.mainContent.select("h1").text() shouldBe heading

      "have the correct title" in:
        doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

      "show the correct p1 text with entity type" in:
        doc.mainContent.select("p.govuk-body").first().text() should include(s"current ${testCase.p1EntityType} of $entityName")

      "show the p2 text" in:
        doc.mainContent.select("p.govuk-body").text() should include("Update your Companies House")

      "show the p3 text" in:
        doc.mainContent.select("p.govuk-body").text() should include("You can then continue")

      "contain the save and come back later button" in:
        val button = doc.mainContent.select("a.govuk-button, button.govuk-button").first()
        button.text() shouldBe "Save and come back later"
        button.attr("href") shouldBe AppRoutes.apply.SaveForLaterController.show.url
