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

package uk.gov.hmrc.agentregistrationfrontend.views.apply.listdetails

import com.softwaremill.quicklens.modify
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistrationfrontend.action.ApplicantActions.DataWithApplication
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.listdetails.EnterIndividualNameComplexPage

class EnterIndividualNameComplexPageSpec
extends ViewSpec:

  private val firstPartnerKey = "first"
  private val nextPartnerKey = "subsequent"

  object ExpectedStrings:

    val heading = "Tell us about the partners of Test Company Name"
    val firstPartnerLabel = "What is the full name of the first partner?"
    val nextPartnerLabel = "What is the full name of the next partner?"

  val viewTemplate: EnterIndividualNameComplexPage = app.injector.instanceOf[EnterIndividualNameComplexPage]
  implicit val agentApplicationRequest: RequestWithData[DataWithApplication] = tdAll.makeAgentApplicationRequest(
    agentApplication =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividualsNeedsPadding
  )

  final case class TestCaseForComplexPage(
    ordinalKey: String,
    numberOfRequiredKeyIndividuals: SixOrMore,
    expectedPaddingBullet1: String,
    expectedPaddingBullet2: String
  )

  val ordinalKeys = List(firstPartnerKey, nextPartnerKey)

  val paddingCases: List[(Int, String, String)] = List(
    (1, "the partner responsible for tax advice", "4 other partners"),
    (2, "the 2 partners responsible for tax advice", "3 other partners"),
    (3, "the 3 partners responsible for tax advice", "2 other partners"),
    (4, "the 4 partners responsible for tax advice", "1 other partner")
  )

  val testCases: List[TestCaseForComplexPage] = ordinalKeys.flatMap: ordinal =>
    paddingCases.map:
      case (n, bullet1, bullet2) =>
        TestCaseForComplexPage(
          ordinalKey = ordinal,
          numberOfRequiredKeyIndividuals = tdAll.sixOrMoreKeyIndividuals
            .modify(_.numberOfKeyIndividualsResponsibleForTaxMatters)
            .setTo(n),
          expectedPaddingBullet1 = bullet1,
          expectedPaddingBullet2 = bullet2
        )

  testCases.foreach: testCase =>
    val n: Int = testCase.numberOfRequiredKeyIndividuals.numberOfKeyIndividualsResponsibleForTaxMatters
    s"EnterIndividualNameComplexPage for $n individuals using ordinal key '${testCase.ordinalKey}'" should:
      val doc: Document = Jsoup.parse(viewTemplate(
        form = IndividualNameForm.form,
        ordinalKey = testCase.ordinalKey,
        numberOfRequiredKeyIndividuals = testCase.numberOfRequiredKeyIndividuals,
        entityName = tdAll.companyName
      ).body)

      val expectedLabel: String =
        testCase.ordinalKey match
          case `firstPartnerKey` => ExpectedStrings.firstPartnerLabel
          case `nextPartnerKey` => ExpectedStrings.nextPartnerLabel

      s"contain expected content for $n individuals using ordinal key '${testCase.ordinalKey}'" in:
        doc.mainContent shouldContainContent
          s"""
             |Partner and tax adviser information
             |Tell us about the partners of Test Company Name
             |We need the names of:
             |${testCase.expectedPaddingBullet1}
             |${testCase.expectedPaddingBullet2}
             |What we mean by ‘responsible for tax advice’
             |A partner is responsible for tax advice if they have:
             |material responsibility for tax advice activities
             |significant authority over HMRC interactions
             |$expectedLabel
             |Save and continue
             |Save and come back later
             |Is this page not working properly? (opens in new tab)
             |""".stripMargin

      s"have the correct title for $n individuals using ordinal key '${testCase.ordinalKey}'" in:
        doc.title() shouldBe s"${ExpectedStrings.heading} - Apply for an agent services account - GOV.UK"

      s"have the correct form label for ordinal key '${testCase.ordinalKey}'" in:
        doc
          .mainContent
          .selectOrFail("form label[for='individualName']")
          .selectOnlyOneElementOrFail()
          .text() shouldBe expectedLabel

      s"render a save and continue button for $n individuals using ordinal '${testCase.ordinalKey}'" in:
        doc
          .mainContent
          .selectOrFail(s"form button[value='${SaveAndContinue.toString}']")
          .selectOnlyOneElementOrFail()
          .text() shouldBe "Save and continue"

      s"render a save and come back later button for $n individuals using ordinal '${testCase.ordinalKey}'" in:
        doc
          .mainContent
          .selectOrFail(s"form button[value=${SaveAndComeBackLater.toString}]")
          .selectOnlyOneElementOrFail()
          .text() shouldBe "Save and come back later"

      s"render a form error when the form contains an error for $n individuals using ordinal key '${testCase.ordinalKey}'" in:
        val field = IndividualNameForm.key
        val errorMessage = "Enter the full name of the partner"
        val formWithError = IndividualNameForm.form
          .withError(field, errorMessage)
        behavesLikePageWithErrorHandling(
          field = field,
          errorMessage = errorMessage,
          errorDoc = Jsoup.parse(viewTemplate(
            form = formWithError,
            ordinalKey = testCase.ordinalKey,
            numberOfRequiredKeyIndividuals = testCase.numberOfRequiredKeyIndividuals,
            entityName = tdAll.companyName
          ).body),
          heading = ExpectedStrings.heading
        )
