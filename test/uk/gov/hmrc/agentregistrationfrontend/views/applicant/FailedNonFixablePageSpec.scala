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
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationRiskingResponse
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.FailedNonFixablePage

class FailedNonFixablePageSpec
extends ViewSpec:

  val viewTemplate: FailedNonFixablePage = app.injector.instanceOf[FailedNonFixablePage]
  val agentApplication: AgentApplication =
    tdAll
      .agentApplicationLlp
      .afterDeclarationSubmitted

  object failedNonFixableResponse:

    val allIndividualsHaveFailures: ApplicationRiskingResponse = tdAll.applicationRiskingResponse.failedNonFixableResponse
    val noIndividualsWithFailures: ApplicationRiskingResponse = tdAll.applicationRiskingResponse.failedNonFixableResponse.copy(
      individuals = tdAll.applicationRiskingResponse.failedNonFixableResponse.individuals.map(_.copy(failures = Some(List.empty)))
    )

  val entityFailureMessages: Map[String, String] = Map(
    "duplicatedMessage" -> "the business has missing tax returns in their HMRC record", // all three entity failures have this same failure message
    "AnyIndividualFailures" -> "one or more relevant individuals linked to the application do not meet the registration conditions"
  )

  val docWithIndividualFailures: Document = Jsoup.parse(
    viewTemplate(
      applicationRiskingResponse = failedNonFixableResponse.allIndividualsHaveFailures,
      agentApplication = agentApplication,
      entityName = "Test Company Name"
    ).body
  )
  val renderedEntityFailures: Elements = docWithIndividualFailures.selectOrFail("#entity-reasons").select("li")

  val docWithNoIndividualFailures: Document = Jsoup.parse(
    viewTemplate(
      applicationRiskingResponse = failedNonFixableResponse.noIndividualsWithFailures,
      agentApplication = agentApplication,
      entityName = "Test Company Name"
    ).body
  )
  val renderedEntityFailuresWithNoIndividualFailures: Elements = docWithNoIndividualFailures.selectOrFail("#entity-reasons").select("li")

  "FailedNonFixablePage when individuals have failures" should:

    "have expected content" in:
      docWithIndividualFailures.mainContent shouldContainContent
        s"""
           |Application outcome
           |Test Company Name does not meet the registration conditions
           |Your application for an agent services account cannot be approved (refused under Section 230 of the Finance Act 2026).
           |This is because:
           |the business has missing tax returns in their HMRC record
           |one or more relevant individuals linked to the application do not meet the registration conditions
           |Relevant individuals who do not meet the registration conditions
           |Steve Austin
           |Records indicate that Steve Austin:
           |has one or more overdue liabilitiesis actively disqualified on Companies house
           |Beverly Hills
           |Records indicate that Beverly Hills:
           |has one or more relevant returns outstanding
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
      docWithIndividualFailures.title() shouldBe "Test Company Name does not meet the registration conditions - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      docWithIndividualFailures.h1 shouldBe "Test Company Name does not meet the registration conditions"

    "have a failure list item for the entity about individual failures" in:
      renderedEntityFailures.last().text() shouldBe entityFailureMessages("AnyIndividualFailures")

    "de-dupe all identical messages" in:
      renderedEntityFailures.eachText() should contain only (
        entityFailureMessages("duplicatedMessage"), // this should only appear once even though it is the message for three different failure codes
        entityFailureMessages("AnyIndividualFailures")
      )

    "print the heading required when individuals have failures" in:
      docWithIndividualFailures.mainContent.selectOrFail(
        "h2#individual-failures"
      ).text() shouldBe "Relevant individuals who do not meet the registration conditions"

    "print a list of unique failures for each individual with failures" in:
      failedNonFixableResponse.allIndividualsHaveFailures.individuals.foreach: individual =>
        docWithIndividualFailures.selectOrFail(s"#${individual.personReference.value}-reasons").select("li").size shouldBe individual.failures.getOrElse(
          List.empty
        ).size

    "should contain a link to the appeals guidance" in:
      val hmrcStandardLink: TestLink =
        docWithIndividualFailures
          .mainContent
          .selectOrFail("a.govuk-link")
          .get(0)
          .toLink

      hmrcStandardLink shouldBe TestLink(
        text = "request a review or appeal the decision (opens in a new tab)",
        href = "https://www.gov.uk/guidance/if-you-disagree-with-hmrcs-decision-about-your-tax-adviser-registration"
      )

  "FailedNonFixablePage when no individuals have failures" should:

    "have expected content" in:

      docWithNoIndividualFailures.mainContent shouldContainContent
        s"""
           |Application outcome
           |Test Company Name does not meet the registration conditions
           |Your application for an agent services account cannot be approved (refused under Section 230 of the Finance Act 2026).
           |This is because:
           |the business has missing tax returns in their HMRC record
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
      docWithNoIndividualFailures.title() shouldBe "Test Company Name does not meet the registration conditions - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      docWithNoIndividualFailures.h1 shouldBe "Test Company Name does not meet the registration conditions"

    "not have a failure list item for the entity about individual failures" in:
      renderedEntityFailuresWithNoIndividualFailures.last().text() should not be entityFailureMessages("AnyIndividualFailures")

    "de-dupe all identical messages" in:
      renderedEntityFailuresWithNoIndividualFailures.eachText() should contain only (
        entityFailureMessages("duplicatedMessage") // this should only appear once even though it is the message for three different failure codes
      )
