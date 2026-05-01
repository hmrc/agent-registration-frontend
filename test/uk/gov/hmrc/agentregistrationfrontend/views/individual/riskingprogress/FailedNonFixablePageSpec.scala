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

package uk.gov.hmrc.agentregistrationfrontend.views.individual.riskingprogress

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.RiskedIndividual
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingprogress.FailedNonFixablePage

class FailedNonFixablePageSpec
extends ViewSpec:

  val viewTemplate: FailedNonFixablePage = app.injector.instanceOf[FailedNonFixablePage]
  val agentApplication: AgentApplication =
    tdAll
      .agentApplicationLlp
      .afterDeclarationSubmitted

  val riskedIndividualFailedNonFixable = RiskedIndividual(
    personReference = PersonReference("PREF0"),
    individualName = tdAll.individualName,
    failures = Seq(IndividualFailure._6)
  )

  val doc: Document = Jsoup.parse(
    viewTemplate(
      riskedIndividual = riskedIndividualFailedNonFixable,
      agentApplication = agentApplication,
      entityName = "Test Company Name"
    ).body
  )

  val docWithMoreThanOneFailure: Document = Jsoup.parse(
    viewTemplate(
      riskedIndividual = riskedIndividualFailedNonFixable.copy(
        failures = Seq(IndividualFailure._6, IndividualFailure._7)
      ),
      agentApplication = agentApplication,
      entityName = "Test Company Name"
    ).body
  )

  "FailedNonFixablePage when individuals have failures" should:

    "have expected content when only one failure" in:

      doc.mainContent shouldContainContent
        s"""
           |Application outcome
           |Records show that you do not meet the registration conditions
           |We have contacted Test Company Name to explain you have not met the registration conditions. This is because records indicate that you are actively disqualified on Companies House.
           |What you need to do
           |You should speak to the business about this decision
           |If the business disagrees with this decision, they can raise it with HMRC
           |Print this page
           |"""
          .stripMargin

    "have expected content when more than one failure" in:
      docWithMoreThanOneFailure.mainContent shouldContainContent
        s"""
           |Application outcome
           |Records show that you do not meet the registration conditions
           |We have contacted Test Company Name to explain you have not met the registration conditions. This is because records indicate that:
           |you are actively disqualified on Companies Houseyou are formally insolvent
           |What you need to do
           |You should speak to the business about this decision
           |If the business disagrees with this decision, they can raise it with HMRC
           |Print this page
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "Records show that you do not meet the registration conditions - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Records show that you do not meet the registration conditions"
