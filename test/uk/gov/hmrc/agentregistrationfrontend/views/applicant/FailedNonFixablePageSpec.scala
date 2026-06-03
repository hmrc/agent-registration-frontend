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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.FailedNonFixablePage

class FailedNonFixablePageSpec
extends ViewSpec:

  val viewTemplate: FailedNonFixablePage = app.injector.instanceOf[FailedNonFixablePage]
  val agentApplication: AgentApplication =
    tdAll
      .agentApplicationLlp
      .afterDeclarationSubmitted

  val personReferenceOne = PersonReference("PREF0")
  val personReferenceTwo = PersonReference("PREF1")

  object failedNonFixableResponse:

    val applicantFailedNoIndividualFailures: RiskingProgress.FailedNonFixable = tdAll.applicationRiskingResponse.failedNonFixableFailedApplicantOnly
    val allIndividualsNonFixableFailures: RiskingProgress.FailedNonFixable = tdAll.applicationRiskingResponse.failedNonFixableIndividualsOnly
    val allNonFixableFailures: RiskingProgress.FailedNonFixable = tdAll.applicationRiskingResponse.allFailedNonFixable
    val duplicateEntityFailures: RiskingProgress.FailedNonFixable = tdAll.applicationRiskingResponse.failedNonFixableWithDuplicates

  val entityFailureMessages: Map[String, String] = Map(
    "duplicatedMessage" -> "our records show that the business is formally insolvent", // all three entity failures have this same failure message
    "AnyIndividualFailures" -> "one or more relevant individuals linked to the application do not meet the registration conditions"
  )

  val docWithAllNonFixableFailures: Document = Jsoup.parse(
    viewTemplate(
      failedNonFixable = failedNonFixableResponse.allNonFixableFailures,
      agentApplication = agentApplication,
      entityName = "Test Company Name"
    ).body
  )

  val docWithIndividualNonFixableFailures: Document = Jsoup.parse(
    viewTemplate(
      failedNonFixable = failedNonFixableResponse.allIndividualsNonFixableFailures,
      agentApplication = agentApplication,
      entityName = "Test Company Name"
    ).body
  )

  val docWithApplicantOnlyNonFixableFailures: Document = Jsoup.parse(
    viewTemplate(
      failedNonFixable = failedNonFixableResponse.applicantFailedNoIndividualFailures,
      agentApplication = agentApplication,
      entityName = "Test Company Name"
    ).body
  )

  val docWithDuplicateFailures: Document = Jsoup.parse(
    viewTemplate(
      failedNonFixable = failedNonFixableResponse.duplicateEntityFailures,
      agentApplication = agentApplication,
      entityName = "Test Company Name"
    ).body
  )

  val renderedEntityFailuresWithNoIndividualFailures: Elements = docWithDuplicateFailures.selectOrFail("#entity-reasons").select("li")

  "FailedNonFixablePage when individuals have failures" should:
    "have expected content" in:
      docWithIndividualNonFixableFailures.mainContent shouldContainContent
        s"""
           |Application outcome
           |Test Company Name does not meet the registration conditions
           |Your application for an agent services account cannot be approved (refused under Section 230 of the Finance Act 2026).
           |This is because one or more relevant individuals linked to the application do not meet the registration conditions.
           |Relevant individuals who do not meet the registration conditions
           |Steve Austin
           |Records indicate that Steve Austin:
           |has one or more overdue liabilitiesis actively disqualified on Companies house
           |Beverly Hills
           |Records indicate that Beverly Hills has one or more relevant returns outstanding.
           |Failure to meet the registration conditions
           |Test Company Name will not be given an agent services account on this occasion.
           |The application will be deleted 45 days after the date we emailed you about this outcome, to comply with our data retention policy.
           |What to do if you disagree
           |If the information in your application was incorrect, or your circumstances change and you think you now meet the registration conditions, you can apply again.
           |If you disagree with the outcome, you can request a review or appeal the decision (opens in a new tab).
           |Print this page
           |"""
          .stripMargin

    "have the correct title" in:
      docWithIndividualNonFixableFailures.title() shouldBe "Test Company Name does not meet the registration conditions - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      docWithIndividualNonFixableFailures.h1 shouldBe "Test Company Name does not meet the registration conditions"

    "display each individuals failure with entity failures" in:
      val paragraphElements = docWithIndividualNonFixableFailures.selectOrFail("p.govuk-body")
      val personReferenceOne = PersonReference("PREF0")
      val entityFailure = paragraphElements.get(1)
      val individualFailuresList = docWithIndividualNonFixableFailures.selectOrFail(s"#${personReferenceOne.value}-reasons").select("li")
      val individualSingleFailure = paragraphElements.get(3)

      individualFailuresList.size() shouldBe 2
      individualSingleFailure.text() shouldBe "Records indicate that Beverly Hills has one or more relevant returns outstanding."
      entityFailure.text() shouldBe "This is because one or more relevant individuals " +
        "linked to the application do not meet the registration conditions."

    "print the heading required when individuals have failures" in:
      docWithIndividualNonFixableFailures.mainContent.selectOrFail(
        "h2#individual-failures"
      ).text() shouldBe "Relevant individuals who do not meet the registration conditions"

    "correctly print the failures for each individual" in:
      val personOneFailures = docWithIndividualNonFixableFailures.selectOrFail(s"#${personReferenceOne.value}-reasons")
      val personTwoFailures = docWithIndividualNonFixableFailures.selectOrFail(s"#${personReferenceTwo.value}-reasons")

      personOneFailures
        .select("li")
        .text() shouldBe "has one or more overdue liabilities " +
        "is actively disqualified on Companies house"

      personTwoFailures
        .select("p")
        .text() shouldBe "Records indicate that Beverly Hills has one or more relevant returns outstanding."

    "should contain a link to the appeals guidance" in:
      val hmrcStandardLink: TestLink =
        docWithIndividualNonFixableFailures
          .mainContent
          .selectOrFail("a.govuk-link")
          .get(0)
          .toLink

      hmrcStandardLink shouldBe TestLink(
        text = "request a review or appeal the decision (opens in a new tab)",
        href = "https://www.gov.uk/guidance/if-you-disagree-with-hmrcs-decision-about-your-tax-adviser-registration"
      )

  "FailedNonFixablePage when no individuals have failures and the applicant has failed" should:
    "have expected content" in:
      docWithApplicantOnlyNonFixableFailures.mainContent shouldContainContent
        s"""
           |Application outcome
           |Test Company Name does not meet the registration conditions
           |Your application for an agent services account cannot be approved (refused under Section 230 of the Finance Act 2026).
           |This is because:
           |one or more relevant individuals linked to the application do not meet the registration conditions
           |the business has missing tax returns in their HMRC recordour records show that the business is formally insolvent
           |Failure to meet the registration conditions
           |Test Company Name will not be given an agent services account on this occasion.
           |The application will be deleted 45 days after the date we emailed you about this outcome, to comply with our data retention policy.
           |What to do if you disagree
           |If the information in your application was incorrect, or your circumstances change and you think you now meet the registration conditions, you can apply again.
           |If you disagree with the outcome, you can request a review or appeal the decision (opens in a new tab).
           |Print this page
           |"""
          .stripMargin

    "FailedNonFixablePage when all individuals have non fixable failures and the entity has non fixable failures" should:
      "have expected content" in:
        docWithAllNonFixableFailures.mainContent shouldContainContent
          s"""
             |Application outcome
             |Test Company Name does not meet the registration conditions
             |Your application for an agent services account cannot be approved (refused under Section 230 of the Finance Act 2026).
             |This is because:
             |one or more relevant individuals linked to the application do not meet the registration conditions
             |the business has missing tax returns in their HMRC recordour records show that the business is formally insolvent
             |Relevant individuals who do not meet the registration conditions
             |Steve Austin
             |Records indicate that Steve Austin:
             |has one or more overdue liabilitiesis actively disqualified on Companies house
             |Beverly Hills
             |Records indicate that Beverly Hills:
             |has one or more overdue liabilitiesis actively disqualified on Companies house
             |Failure to meet the registration conditions
             |Test Company Name will not be given an agent services account on this occasion.
             |The application will be deleted 45 days after the date we emailed you about this outcome, to comply with our data retention policy.
             |What to do if you disagree
             |If the information in your application was incorrect, or your circumstances change and you think you now meet the registration conditions, you can apply again.
             |If you disagree with the outcome, you can request a review or appeal the decision (opens in a new tab).
             |Print this page
             |"""
            .stripMargin

    "have the correct title" in:
      docWithApplicantOnlyNonFixableFailures.title() shouldBe "Test Company Name does not meet the registration conditions - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      docWithApplicantOnlyNonFixableFailures.h1 shouldBe "Test Company Name does not meet the registration conditions"

    "not have a failure list item for the entity about individual failures" in:
      renderedEntityFailuresWithNoIndividualFailures.last().text() should not be entityFailureMessages("AnyIndividualFailures")

    "not display identical messages for separate failure codes" in:
      renderedEntityFailuresWithNoIndividualFailures.toArray().distinct.length shouldBe renderedEntityFailuresWithNoIndividualFailures.size
