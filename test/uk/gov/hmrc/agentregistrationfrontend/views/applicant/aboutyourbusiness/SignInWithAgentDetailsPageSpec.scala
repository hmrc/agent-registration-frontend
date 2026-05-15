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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.aboutyourbusiness

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.aboutyourbusiness.SignInWithAgentDetailsPage

class SignInWithAgentDetailsPageSpec
extends ViewSpec:

  val viewTemplate: SignInWithAgentDetailsPage = app.injector.instanceOf[SignInWithAgentDetailsPage]

  val signInLink = uri"/agent-registration/sign-in"

  val doc: Document = Jsoup.parse(
    viewTemplate(signInLink).body
  )

  "SignInWithAgentDetailsPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |About your business
          |Sign in with your existing agent account details
          |On the next page, enter the sign in details for your existing agent account.
          |We’ll link your new agent services account to these sign in details.
          |If you have more than one set of sign in details
          |We can link an agent services account to any existing Government Gateway account, as long as:
          |it was set up as an account for agents (not for an individual or non-agent organisation)
          |you can enter the user ID and password when we ask you for them
          |Adding your agent services account to existing sign in details will not affect any other services you access with those sign in details.
          |Continue
          |Is this page not working properly? (opens in new tab)
          |"""
          .stripMargin

    "have the correct caption" in:
      doc.selectOrFail(captionL).text() shouldBe "About your business"

    "have the correct title" in:
      doc.title() shouldBe "Sign in with your existing agent account details - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Sign in with your existing agent account details"

    "render first paragraph" in:
      doc
        .mainContent
        .selectOrFail("p.govuk-body")
        .first()
        .text() shouldBe "On the next page, enter the sign in details for your existing agent account."

    "render second paragraph" in:
      doc
        .mainContent
        .selectOrFail("p.govuk-body")
        .get(1)
        .text() shouldBe "We’ll link your new agent services account to these sign in details."

    "render a continue link styled as a button" in:
      doc.select("a.govuk-button[role=button]").text() shouldBe "Continue"
