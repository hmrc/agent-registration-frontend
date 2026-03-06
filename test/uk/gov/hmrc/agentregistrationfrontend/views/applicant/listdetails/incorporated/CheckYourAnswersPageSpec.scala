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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.CheckYourAnswersPage

class CheckYourAnswersPageSpec
extends ViewSpec:

  val viewTemplate: CheckYourAnswersPage = app.injector.instanceOf[CheckYourAnswersPage]

  private val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterNumberOfConfirmCompaniesHouseOfficers

  private val sixOrMoreOfficers: SixOrMoreOfficers = TestOnlyData.sixOrMoreCompaniesHouseOfficers

  private val caption: String = "LLP members and other tax adviser information"
  private val changeNumberLink: String = "Change the number of LLP members"

  private def render(existingList: List[IndividualProvidedDetails]): Document = Jsoup.parse(viewTemplate(
    sixOrMoreOfficers = sixOrMoreOfficers,
    existingList = existingList,
    agentApplication = agentApplication
  ).body)

  "CheckYourAnswersPage" should:

    "when the list has fewer individuals than required" should:

      val doc: Document = render(List(tdAll.individualProvidedDetails))

      "have the correct title" in:
        doc.title() shouldBe "You have added 1 LLP member - Apply for an agent services account - GOV.UK"

      "contain the caption" in:
        doc.mainContent.select("h2.govuk-caption-l").text() shouldBe caption

      "contain the heading" in:
        doc.mainContent.select("h1").text() shouldBe "You have added 1 LLP member"

      "contain the change number link" in:
        val link = doc.mainContent.select("p.govuk-body a").first()
        link.text() shouldBe changeNumberLink
        link.attr("href") shouldBe AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url

      "contain a summary list with the individual name" in:
        doc.mainContent.select(".hmrc-summary-list__key").text() should include(tdAll.individualProvidedDetails.individualName.value)

      "contain Change and Remove action links" in:
        val actions = doc.mainContent.select(".hmrc-summary-list__actions a")
        actions.toString should include("Change")
        actions.toString should include("Remove")

      "contain the Change action link with correct href" in:
        val changeLink = doc.mainContent.select(".hmrc-summary-list__actions a").first()
        changeLink.attr(
          "href"
        ) shouldBe AppRoutes.apply.listdetails.incoporated.ChangeCompaniesHouseOfficerController.show(tdAll.individualProvidedDetails._id).url

      "contain the Remove action link with correct href" in:
        val removeLink = doc.mainContent.select(".hmrc-summary-list__actions a").get(1)
        removeLink.attr(
          "href"
        ) shouldBe AppRoutes.apply.listdetails.incoporated.RemoveCompaniesHouseOfficerController.show(tdAll.individualProvidedDetails._id).url

      "show inset text about needing more individuals" in:
        doc.mainContent.select(".govuk-inset-text").text() should include("LLP member")

      "show the add another link" in:
        doc.mainContent.select("a").toString should include("Add another LLP member")

    "when the list matches the required count" should:

      // Build a list that matches the required count (including padding)
      val requiredCount = sixOrMoreOfficers.totalListSize
      val fullList =
        (1 to requiredCount).map(i =>
          tdAll.individualProvidedDetails
        ).toList
      val doc: Document = render(fullList)

      "show confirm and continue link" in:
        doc.mainContent.select("a").toString should include("Confirm and continue")

      "not show inset text" in:
        doc.mainContent.select(".govuk-inset-text").text() shouldBe ""

    "when the list is empty" should:

      val doc: Document = render(List.empty)

      "have the correct title for zero items" in:
        doc.title() should include("added 0")

      "show the summary list with no rows" in:
        doc.mainContent.select(".hmrc-summary-list__key").size() shouldBe 0
