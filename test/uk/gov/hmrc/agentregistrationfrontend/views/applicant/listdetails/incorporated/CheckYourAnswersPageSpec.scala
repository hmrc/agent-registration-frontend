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
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.CheckYourAnswersPage

class CheckYourAnswersPageSpec
extends ViewSpec:

  val viewTemplate: CheckYourAnswersPage = app.injector.instanceOf[CheckYourAnswersPage]

  private val sixOrMoreOfficers: SixOrMoreOfficers = tdAll.sixOrMoreCompaniesHouseOfficers

  private def render(
    existingList: List[IndividualProvidedDetails],
    agentApplication: AgentApplication
  ): Document = Jsoup.parse(viewTemplate(
    sixOrMoreOfficers = sixOrMoreOfficers,
    existingList = existingList,
    agentApplication = agentApplication
  ).body)

  case class BusinessTypeTestCase(
    label: String,
    agentApplication: AgentApplication,
    caption: String,
    entityType: String,
    singularTitle: String,
    changeNumberLink: String,
    addAnotherLink: String
  )

  private val testCases = Seq(
    BusinessTypeTestCase(
      label = "LimitedLiabilityPartnership",
      agentApplication = tdAll.agentApplicationLlp.afterNumberOfConfirmCompaniesHouseOfficers,
      caption = "LLP members and other relevant individuals",
      entityType = "LLP member",
      singularTitle = "You have added 1 LLP member",
      changeNumberLink = "Change the number of LLP members",
      addAnotherLink = "Add another LLP member"
    ),
    BusinessTypeTestCase(
      label = "LimitedCompany",
      agentApplication = tdAll.agentApplicationLimitedCompany.afterNumberOfConfirmCompaniesHouseOfficers,
      caption = "Directors and other relevant individuals",
      entityType = "director",
      singularTitle = "You have added 1 director",
      changeNumberLink = "Change the number of directors",
      addAnotherLink = "Add another director"
    ),
    BusinessTypeTestCase(
      label = "LimitedPartnership",
      agentApplication = tdAll.agentApplicationLimitedPartnership.afterNumberOfConfirmCompaniesHouseOfficers,
      caption = "Partners and other relevant individuals",
      entityType = "partner",
      singularTitle = "You have added 1 partner",
      changeNumberLink = "Change the number of partners",
      addAnotherLink = "Add another partner"
    ),
    BusinessTypeTestCase(
      label = "ScottishLimitedPartnership",
      agentApplication = tdAll.agentApplicationScottishLimitedPartnership.afterNumberOfConfirmCompaniesHouseOfficers,
      caption = "Partners and other relevant individuals",
      entityType = "partner",
      singularTitle = "You have added 1 partner",
      changeNumberLink = "Change the number of partners",
      addAnotherLink = "Add another partner"
    )
  )

  for testCase <- testCases do
    s"CheckYourAnswersPage for ${testCase.label}" should:

      "when the list has fewer individuals than required" should:

        val doc: Document = render(List(tdAll.providedDetails.precreated), testCase.agentApplication)

        "have the correct title" in:
          doc.title() shouldBe s"${testCase.singularTitle} - Apply for an agent services account - GOV.UK"

        "contain the caption" in:
          doc.mainContent.select("h2.govuk-caption-l").text() shouldBe testCase.caption

        "contain the heading" in:
          doc.mainContent.select("h1").text() shouldBe testCase.singularTitle

        "contain the change number link with correct text" in:
          val link = doc.mainContent.select("p.govuk-body a").first()
          link.text() shouldBe testCase.changeNumberLink
          link.attr("href") shouldBe AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url

        "contain a summary list with the individual name" in:
          doc.mainContent.select(".hmrc-summary-list__key").text() should include(tdAll.providedDetails.precreated.individualName.value)

        "contain Change and Remove action links" in:
          val actions = doc.mainContent.select(".hmrc-summary-list__actions a")
          actions.toString should include("Change")
          actions.toString should include("Remove")

        "show inset text with correct entity type" in:
          doc.mainContent.select(".govuk-inset-text").text() should include(testCase.entityType)

        "show the add another link with correct text" in:
          doc.mainContent.select("a").toString should include(testCase.addAnotherLink)

      "when the list matches the required count" should:

        val requiredCount = sixOrMoreOfficers.totalListSize
        val fullList = (1 to requiredCount).map(_ => tdAll.providedDetails.precreated).toList
        val doc: Document = render(fullList, testCase.agentApplication)

        "show confirm and continue link" in:
          doc.mainContent.select("a").toString should include("Confirm and continue")

        "not show inset text" in:
          doc.mainContent.select(".govuk-inset-text").text() shouldBe ""

      "when the list is empty" should:

        val doc: Document = render(List.empty, testCase.agentApplication)

        "have the correct title for zero items" in:
          doc.title() should include("added 0")
