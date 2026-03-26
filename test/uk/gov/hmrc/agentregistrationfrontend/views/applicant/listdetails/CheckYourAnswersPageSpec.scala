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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.getNumberOfCompaniesHouseOfficers
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.CheckYourAnswersPage

class CheckYourAnswersPageSpec
extends ViewSpec:

  val viewTemplate: CheckYourAnswersPage = app.injector.instanceOf[CheckYourAnswersPage]

  private def render(
    agentApplication: AgentApplication,
    nonIncorporatedIndividualsList: List[IndividualProvidedDetails],
    otherRelevantIndividualsList: List[IndividualProvidedDetails]
  ): Document = Jsoup.parse(viewTemplate(
    agentApplication = agentApplication,
    nonIncorporatedIndividualsList = nonIncorporatedIndividualsList,
    otherRelevantIndividualsList = otherRelevantIndividualsList
  ).body)

  case class BusinessTypeTestCase(
    label: String,
    agentApplication: AgentApplication,
    caption: String,
    numberOfPartnersLabel: String,
    partnerNamesLabel: String,
    changeNumberHref: String,
    changeNamesHref: String
  )

  private val nonIncorporatedTestCases = Seq(
    BusinessTypeTestCase(
      label = "Partnership",
      agentApplication = tdAll.agentApplicationGeneralPartnership.afterConfirmOtherRelevantIndividualsNo,
      caption = "Partners and other relevant tax advisers",
      numberOfPartnersLabel = "Number of partners",
      partnerNamesLabel = "Partner names",
      changeNumberHref = AppRoutes.apply.listdetails.nonincorporated.NumberOfKeyIndividualsController.show.url,
      changeNamesHref = AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show.url
    )
  )

  private val incorporatedTestCases = Seq(
    BusinessTypeTestCase(
      label = "LimitedLiabilityPartnership",
      agentApplication = tdAll.agentApplicationLlp.afterNumberOfConfirmCompaniesHouseOfficers.copy(hasOtherRelevantIndividuals = Some(false)),
      caption = "LLP members and other tax adviser information",
      numberOfPartnersLabel = "Number of LLP members",
      partnerNamesLabel = "LLP member names",
      changeNumberHref = AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url,
      changeNamesHref = AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url
    ),
    BusinessTypeTestCase(
      label = "LimitedCompany",
      agentApplication = tdAll.agentApplicationLimitedCompany.afterNumberOfConfirmCompaniesHouseOfficers.copy(hasOtherRelevantIndividuals = Some(false)),
      caption = "Directors and other tax adviser information",
      numberOfPartnersLabel = "Number of directors",
      partnerNamesLabel = "Director names",
      changeNumberHref = AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url,
      changeNamesHref = AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url
    ),
    BusinessTypeTestCase(
      label = "LimitedPartnership",
      agentApplication = tdAll.agentApplicationLimitedPartnership.afterNumberOfConfirmCompaniesHouseOfficers.copy(hasOtherRelevantIndividuals = Some(false)),
      caption = "Partners and other tax adviser information",
      numberOfPartnersLabel = "Number of partners",
      partnerNamesLabel = "Partner names",
      changeNumberHref = AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url,
      changeNamesHref = AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url
    ),
    BusinessTypeTestCase(
      label = "ScottishLimitedPartnership",
      agentApplication = tdAll.agentApplicationScottishLimitedPartnership.afterNumberOfConfirmCompaniesHouseOfficers.copy(hasOtherRelevantIndividuals =
        Some(false)
      ),
      caption = "Partners and other tax adviser information",
      numberOfPartnersLabel = "Number of partners",
      partnerNamesLabel = "Partner names",
      changeNumberHref = AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url,
      changeNamesHref = AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url
    )
  )

  private val allTestCases = nonIncorporatedTestCases ++ incorporatedTestCases

  for testCase <- allTestCases do
    s"CheckYourAnswersPage for ${testCase.label}" should:

      "when individuals and no other relevant individuals" should:

        val partnersList = List(tdAll.individualProvidedDetails)
        val doc: Document = render(
          testCase.agentApplication,
          partnersList,
          List.empty
        )

        "have the correct title" in:
          doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"

        "contain the caption" in:
          doc.mainContent.select("h2.govuk-caption-l").text() shouldBe testCase.caption

        "contain the heading" in:
          doc.mainContent.select("h1").text() shouldBe "Check your answers"

        "show the number of partners row with correct label" in:
          val keys = doc.mainContent.select(".govuk-summary-list__key")
          keys.get(0).text() shouldBe testCase.numberOfPartnersLabel

        "show the number of partners value" in:
          val values = doc.mainContent.select(".govuk-summary-list__value")
          val expectedNumber =
            testCase.agentApplication match {
              case a: uk.gov.hmrc.agentregistration.shared.AgentApplication.IsIncorporated =>
                a.getNumberOfCompaniesHouseOfficers.map(_.numberOfIndividuals.toString).getOrElse("")
              case _ => partnersList.size.toString
            }
          values.get(0).text() shouldBe expectedNumber

        "show the change number link with correct href" in:
          val changeLinks = doc.mainContent.select(".govuk-summary-list__actions a")
          changeLinks.get(0).attr("href") shouldBe testCase.changeNumberHref

        "show the partner names row with correct label" in:
          val keys = doc.mainContent.select(".govuk-summary-list__key")
          keys.get(1).text() shouldBe testCase.partnerNamesLabel

        "show the partner names value" in:
          val values = doc.mainContent.select(".govuk-summary-list__value")
          values.get(1).text() should include(tdAll.individualProvidedDetails.individualName.value)

        "show the change names link with correct href" in:
          val changeLinks = doc.mainContent.select(".govuk-summary-list__actions a")
          changeLinks.get(1).attr("href") shouldBe testCase.changeNamesHref

        "show the has other relevant individuals row with No" in:
          val keys = doc.mainContent.select(".govuk-summary-list__key")
          keys.get(2).text() shouldBe "Other relevant tax advisers"
          val values = doc.mainContent.select(".govuk-summary-list__value")
          values.get(2).text() shouldBe "No"

        "show the confirm and continue button" in:
          doc.mainContent.select("a.govuk-button").text() should include("Confirm and continue")

      "when individuals and other relevant individuals present" should:

        val agentApplicationWithOthers =
          testCase.agentApplication match
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership => a.copy(hasOtherRelevantIndividuals = Some(true))
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp => a.copy(hasOtherRelevantIndividuals = Some(true))
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany => a.copy(hasOtherRelevantIndividuals = Some(true))
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedPartnership => a.copy(hasOtherRelevantIndividuals = Some(true))
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishLimitedPartnership => a.copy(hasOtherRelevantIndividuals = Some(true))
            case a => a // should not happen

        val otherIndividual = tdAll.individualProvidedDetails.copy(isPersonOfControl = false)
        val doc: Document = render(
          agentApplicationWithOthers,
          List(tdAll.individualProvidedDetails),
          List(otherIndividual)
        )

        "show the has other relevant individuals row with Yes" in:
          val keys = doc.mainContent.select(".govuk-summary-list__key")
          keys.get(2).text() shouldBe "Other relevant tax advisers"
          val values = doc.mainContent.select(".govuk-summary-list__value")
          values.get(2).text() shouldBe "Yes"

        "show the other relevant individuals names row" in:
          val keys = doc.mainContent.select(".govuk-summary-list__key")
          keys.get(3).text() shouldBe "Other relevant tax adviser names"

      "when no individuals in the list" should:

        val doc: Document = render(
          testCase.agentApplication,
          List.empty,
          List.empty
        )

        "show the number value" in:
          val values = doc.mainContent.select(".govuk-summary-list__value")
          val expectedNumber =
            testCase.agentApplication match {
              case a: uk.gov.hmrc.agentregistration.shared.AgentApplication.IsIncorporated =>
                a.getNumberOfCompaniesHouseOfficers.map(_.numberOfIndividuals.toString).getOrElse("")
              case _ => "0"
            }
          values.get(0).text() shouldBe expectedNumber

        "not show the partner names row" in:
          val keys = doc.mainContent.select(".govuk-summary-list__key")
          keys.size() shouldBe 2 // number row + has others row, no names row
