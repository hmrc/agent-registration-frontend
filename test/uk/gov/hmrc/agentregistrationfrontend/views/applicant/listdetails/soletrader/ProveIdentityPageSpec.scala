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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.soletrader

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.soletrader.ProveIdentityPage

import java.net.URLEncoder

class ProveIdentityPageSpec
extends ViewSpec:

  val viewTemplate: ProveIdentityPage = app.injector.instanceOf[ProveIdentityPage]
  val agentApplication: AgentApplicationSoleTrader = tdAll.agentApplicationSoleTrader.afterHmrcStandardForAgentsAgreed

  val doc: Document = Jsoup.parse(
    viewTemplate(
      agentApplication = agentApplication,
      hasProvedIdentity = false
    ).body
  )

  val completedDoc: Document = Jsoup.parse(
    viewTemplate(
      agentApplication = agentApplication,
      hasProvedIdentity = true
    ).body
  )

  "ProveIdentityPage" should:

    "have expected content when not completed" in:
      doc.mainContent shouldContainContent
        """
          |Prove your identity
          |Sign in with your personal details
          |You are currently signed in as an agent.
          |We need you to sign in using the details you created for your personal taxes, not the details you use to act as an agent.
          |If you do not have personal sign in details, you can create some.
          |Continue
          |Save and come back later
          |Is this page not working properly? (opens in new tab)
          |"""
          .stripMargin

    "have the correct title when not completed" in:
      doc.title() shouldBe "Sign in with your personal details - Apply for an agent services account - GOV.UK"

    "have the correct h1 when not completed" in:
      doc.h1 shouldBe "Sign in with your personal details"

    "render a continue link to sign out with continue url for the individual journey when not completed" in:
      val continueUrl = s"${thisFrontendBaseUrl}/agent-registration/provide-details/match-application/${agentApplication.linkId.value}"
      val expectedEncodedContinueUrl = URLEncoder.encode(continueUrl, "UTF-8")
      doc
        .mainContent
        .selectOrFail(s"""a[href="/agent-registration/sign-out-with-continue?continueUrl=${expectedEncodedContinueUrl}"]""")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Continue"

  "have expected content when completed" in:
    completedDoc.mainContent shouldContainContent
      """
        |Prove your identity
        |You have proven your identity
        |Continue
        |Save and come back later
        |Is this page not working properly? (opens in new tab)
        |"""
        .stripMargin

  "have the correct title when completed" in:
    completedDoc.title() shouldBe "You have proven your identity - Apply for an agent services account - GOV.UK"

  "have the correct h1 when completed" in:
    completedDoc.h1 shouldBe "You have proven your identity"
