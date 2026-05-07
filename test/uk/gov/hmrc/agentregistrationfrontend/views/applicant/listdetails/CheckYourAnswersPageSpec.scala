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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.CheckYourAnswersPage

class CheckYourAnswersPageSpec
extends ViewSpec:

  val viewTemplate: CheckYourAnswersPage = app.injector.instanceOf[CheckYourAnswersPage]

  private def render(
    agentApplication: AgentApplication,
    listOfKeyIndividuals: List[IndividualProvidedDetails],
    otherRelevantIndividualsList: List[IndividualProvidedDetails]
  ): Document = Jsoup.parse(viewTemplate(
    agentApplication = agentApplication,
    listOfKeyIndividuals = listOfKeyIndividuals,
    listOfOtherRelevantIndividuals = otherRelevantIndividualsList
  ).body)

  case class BusinessTypeTestCase(
    label: String,
    agentApplication: AgentApplication,
    caption: String,
    numberOfKeyIndividualsLabel: String,
    keyIndividualNamesLabel: String,
    changeNumberHref: String,
    changeNamesHref: String
  )

  private val nonIncorporatedTestCases = Seq(
    BusinessTypeTestCase(
      label = "GeneralPartnership",
      agentApplication = tdAll.agentApplicationGeneralPartnership.afterConfirmOtherRelevantIndividualsNo,
      caption = "Partners and other relevant individuals",
      numberOfKeyIndividualsLabel = "Number of partners",
      keyIndividualNamesLabel = "Partner names",
      changeNumberHref = AppRoutes.apply.listdetails.nonincorporated.NumberOfKeyIndividualsController.show.url,
      changeNamesHref = AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show.url
    ),
    BusinessTypeTestCase(
      label = "ScottishPartnership",
      agentApplication = tdAll.agentApplicationScottishPartnership.afterConfirmOtherRelevantIndividualsNo,
      caption = "Partners and other relevant individuals",
      numberOfKeyIndividualsLabel = "Number of partners",
      keyIndividualNamesLabel = "Partner names",
      changeNumberHref = AppRoutes.apply.listdetails.nonincorporated.NumberOfKeyIndividualsController.show.url,
      changeNamesHref = AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show.url
    )
  )

  private val incorporatedTestCases = Seq(
    BusinessTypeTestCase(
      label = "LimitedLiabilityPartnership",
      agentApplication = tdAll.agentApplicationLlp.afterNumberOfConfirmCompaniesHouseOfficers.copy(hasOtherRelevantIndividuals = Some(false)),
      caption = "LLP members and other relevant individuals",
      numberOfKeyIndividualsLabel = "Number of LLP members",
      keyIndividualNamesLabel = "LLP member names",
      changeNumberHref = AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url,
      changeNamesHref = AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url
    ),
    BusinessTypeTestCase(
      label = "LimitedCompany",
      agentApplication = tdAll.agentApplicationLimitedCompany.afterNumberOfConfirmCompaniesHouseOfficers.copy(hasOtherRelevantIndividuals = Some(false)),
      caption = "Directors and other relevant individuals",
      numberOfKeyIndividualsLabel = "Number of directors",
      keyIndividualNamesLabel = "Director names",
      changeNumberHref = AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url,
      changeNamesHref = AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url
    ),
    BusinessTypeTestCase(
      label = "LimitedPartnership",
      agentApplication = tdAll.agentApplicationLimitedPartnership.afterNumberOfConfirmCompaniesHouseOfficers.copy(hasOtherRelevantIndividuals = Some(false)),
      caption = "Partners and other relevant individuals",
      numberOfKeyIndividualsLabel = "Number of partners",
      keyIndividualNamesLabel = "Partner names",
      changeNumberHref = AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url,
      changeNamesHref = AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url
    ),
    BusinessTypeTestCase(
      label = "ScottishLimitedPartnership",
      agentApplication = tdAll.agentApplicationScottishLimitedPartnership.afterNumberOfConfirmCompaniesHouseOfficers.copy(hasOtherRelevantIndividuals =
        Some(false)
      ),
      caption = "Partners and other relevant individuals",
      numberOfKeyIndividualsLabel = "Number of partners",
      keyIndividualNamesLabel = "Partner names",
      changeNumberHref = AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url,
      changeNamesHref = AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url
    )
  )

  // non-incorporated and incorporated are sufficiently different to run separate loops for each
  for testCase <- nonIncorporatedTestCases do
    s"CheckYourAnswersPage for non-incorporated type ${testCase.label}" should:

      "when key individuals and no other relevant individuals" should:

        val listOfKeyIndividuals = List(
          tdAll.providedDetails.precreated,
          tdAll.providedDetails.precreated,
          tdAll.providedDetails.precreated
        )
        val doc: Document = render(
          testCase.agentApplication,
          listOfKeyIndividuals,
          List.empty
        )

        "have the correct title" in:
          doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"

        "contain the caption" in:
          doc.mainContent.select(captionL).text() shouldBe testCase.caption

        "contain the heading" in:
          doc.mainContent.select("h1").text() shouldBe "Check your answers"

        "render the correct summary list" in:
          val expectedSummaryList: TestSummaryList = TestSummaryList(
            List(
              TestSummaryRow(
                key = "Number of partners",
                value = "3",
                action = testCase.changeNumberHref,
                changeLinkAccessibleContent = "Change Number of partners"
              ),
              TestSummaryRow(
                key = "Partner names",
                value = "Test Name Test Name Test Name",
                action = testCase.changeNamesHref,
                changeLinkAccessibleContent = "Change Partner names"
              ),
              TestSummaryRow(
                key = "Other relevant individuals",
                value = "No",
                action = AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.show.url,
                changeLinkAccessibleContent = "Change Other relevant individuals"
              )
            )
          )
          doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

        "show the confirm and continue button" in:
          doc.mainContent.select("a.govuk-button").text() should include("Confirm and continue")

      "when individuals and other relevant individuals present" should:

        val agentApplicationWithOthers =
          testCase.agentApplication match
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership => a.copy(hasOtherRelevantIndividuals = Some(true))
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishPartnership => a.copy(hasOtherRelevantIndividuals = Some(true))
            case a => a // should not happen

        val otherIndividual = tdAll.providedDetails.precreated.copy(isPersonOfControl = false)
        val doc: Document = render(
          agentApplicationWithOthers,
          List(
            tdAll.providedDetails.precreated,
            tdAll.providedDetails.precreated,
            tdAll.providedDetails.precreated
          ),
          List(otherIndividual)
        )

        "render the correct summary list" in:
          val expectedSummaryList: TestSummaryList = TestSummaryList(
            List(
              TestSummaryRow(
                key = "Number of partners",
                value = "3",
                action = testCase.changeNumberHref,
                changeLinkAccessibleContent = "Change Number of partners"
              ),
              TestSummaryRow(
                key = "Partner names",
                value = "Test Name Test Name Test Name",
                action = testCase.changeNamesHref,
                changeLinkAccessibleContent = "Change Partner names"
              ),
              TestSummaryRow(
                key = "Other relevant individuals",
                value = "Yes",
                action = AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.show.url,
                changeLinkAccessibleContent = "Change Other relevant individuals"
              ),
              TestSummaryRow(
                key = "Other relevant individual names",
                value = "Test Name",
                action = AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url,
                changeLinkAccessibleContent = "Change Other relevant individual names"
              )
            )
          )
          doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

      // we now force hasRelevantIndividuals to be true when zero officers are found
      "when no individuals in the key individuals list" should:
        val agentApplicationWithZeroKeyIndividuals =
          testCase.agentApplication match
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership =>
              a.copy(
                hasOtherRelevantIndividuals = Some(true),
                numberOfIndividuals = Some(FiveOrLess(0))
              )
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishPartnership =>
              a.copy(
                hasOtherRelevantIndividuals = Some(true),
                numberOfIndividuals = Some(FiveOrLess(0))
              )
            case a => a // should not happen
        val doc: Document = render(
          agentApplicationWithZeroKeyIndividuals,
          List.empty,
          List(tdAll.providedDetails.precreated.copy(isPersonOfControl = false))
        )

        "render the correct summary list" in:
          val expectedSummaryList: TestSummaryList = TestSummaryList(
            List(
              TestSummaryRow(
                key = "Number of partners",
                value = "0",
                action = testCase.changeNumberHref,
                changeLinkAccessibleContent = "Change Number of partners"
              ),
              TestSummaryRow(
                key = "Relevant individual names",
                value = "Test Name",
                action = AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url,
                changeLinkAccessibleContent = "Change Relevant individual names"
              )
            )
          )
          doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

  for testCase <- incorporatedTestCases do
    s"CheckYourAnswersPage for incorporated type ${testCase.label}" should:

      "when key individuals and no other relevant individuals" should:

        val listOfKeyIndividuals = List(
          tdAll.providedDetails.precreated,
          tdAll.providedDetails.precreated,
          tdAll.providedDetails.precreated
        )
        val doc: Document = render(
          testCase.agentApplication,
          listOfKeyIndividuals,
          List.empty
        )

        "have the correct title" in:
          doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"

        "contain the caption" in:
          doc.mainContent.select("h2.govuk-caption-l").text() shouldBe testCase.caption

        "contain the heading" in:
          doc.mainContent.select("h1").text() shouldBe "Check your answers"

        "render the correct summary list" in:
          val expectedSummaryList: TestSummaryList = TestSummaryList(
            List(
              TestSummaryRow(
                key = testCase.numberOfKeyIndividualsLabel,
                value = "3",
                action = testCase.changeNumberHref,
                changeLinkAccessibleContent = s"Change ${testCase.numberOfKeyIndividualsLabel}"
              ),
              TestSummaryRow(
                key = testCase.keyIndividualNamesLabel,
                value = "Test Name Test Name Test Name",
                action = testCase.changeNamesHref,
                changeLinkAccessibleContent = s"Change ${testCase.keyIndividualNamesLabel}"
              ),
              TestSummaryRow(
                key = "Other relevant individuals",
                value = "No",
                action = AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.show.url,
                changeLinkAccessibleContent = "Change Other relevant individuals"
              )
            )
          )
          doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

        "show the confirm and continue button" in:
          doc.mainContent.select("a.govuk-button").text() should include("Confirm and continue")

      "when individuals and other relevant individuals present" should:

        val agentApplicationWithOthers =
          testCase.agentApplication match
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp => a.copy(hasOtherRelevantIndividuals = Some(true))
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany => a.copy(hasOtherRelevantIndividuals = Some(true))
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedPartnership => a.copy(hasOtherRelevantIndividuals = Some(true))
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishLimitedPartnership => a.copy(hasOtherRelevantIndividuals = Some(true))
            case a => a // should not happen

        val otherIndividual = tdAll.providedDetails.precreated.copy(isPersonOfControl = false)
        val doc: Document = render(
          agentApplicationWithOthers,
          List(
            tdAll.providedDetails.precreated,
            tdAll.providedDetails.precreated,
            tdAll.providedDetails.precreated
          ),
          List(otherIndividual)
        )

        "render the correct summary list" in:
          val expectedSummaryList: TestSummaryList = TestSummaryList(
            List(
              TestSummaryRow(
                key = testCase.numberOfKeyIndividualsLabel,
                value = "3",
                action = testCase.changeNumberHref,
                changeLinkAccessibleContent = s"Change ${testCase.numberOfKeyIndividualsLabel}"
              ),
              TestSummaryRow(
                key = testCase.keyIndividualNamesLabel,
                value = "Test Name Test Name Test Name",
                action = testCase.changeNamesHref,
                changeLinkAccessibleContent = s"Change ${testCase.keyIndividualNamesLabel}"
              ),
              TestSummaryRow(
                key = "Other relevant individuals",
                value = "Yes",
                action = AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.show.url,
                changeLinkAccessibleContent = "Change Other relevant individuals"
              ),
              TestSummaryRow(
                key = "Other relevant individual names",
                value = "Test Name",
                action = AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url,
                changeLinkAccessibleContent = "Change Other relevant individual names"
              )
            )
          )
          doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

      // we now force hasRelevantIndividuals to be true when zero officers are found
      "when no individuals in the key individuals list" should:
        val agentApplicationWithZeroKeyIndividuals =
          testCase.agentApplication match
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp =>
              a.copy(
                hasOtherRelevantIndividuals = Some(true),
                numberOfIndividuals = Some(FiveOrLessOfficers(0, true))
              )
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany =>
              a.copy(
                hasOtherRelevantIndividuals = Some(true),
                numberOfIndividuals = Some(FiveOrLessOfficers(0, true))
              )
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedPartnership =>
              a.copy(
                hasOtherRelevantIndividuals = Some(true),
                numberOfIndividuals = Some(FiveOrLessOfficers(0, true))
              )
            case a: uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishLimitedPartnership =>
              a.copy(
                hasOtherRelevantIndividuals = Some(true),
                numberOfIndividuals = Some(FiveOrLessOfficers(0, true))
              )
            case a => a // should not happen
        val doc: Document = render(
          agentApplicationWithZeroKeyIndividuals,
          List.empty,
          List(tdAll.providedDetails.precreated.copy(isPersonOfControl = false))
        )

        "render the correct summary list" in:
          val expectedSummaryList: TestSummaryList = TestSummaryList(
            List(
              TestSummaryRow(
                key = "Relevant individual names",
                value = "Test Name",
                action = AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url,
                changeLinkAccessibleContent = "Change Relevant individual names"
              )
            )
          )
          doc.mainContent.extractSummaryList() shouldBe expectedSummaryList
