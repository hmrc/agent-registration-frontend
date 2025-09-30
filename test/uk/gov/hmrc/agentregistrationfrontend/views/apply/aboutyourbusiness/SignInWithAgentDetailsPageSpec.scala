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

package uk.gov.hmrc.agentregistrationfrontend.views.apply.aboutyourbusiness

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.SignInWithAgentDetailsPage

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
          |Sign in with your agent account details
          |Sign in using the details for your existing agent account.
          |Your new agent services account will then be linked to those sign in details.
          |If you have more than one set of sign in details
          |Choose the one you want to be linked to your new agent services account.
          |Continue
          |"""
          .stripMargin

    "have the correct caption" in:
      doc.selectOrFail(captionL).text() shouldBe "About your business"

    "have the correct title" in:
      doc.title() shouldBe "Sign in with your agent account details - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Sign in with your agent account details"

    "render first paragraph" in:
      doc
        .mainContent
        .selectOrFail("p.govuk-body")
        .first()
        .text() shouldBe "Sign in using the details for your existing agent account."

    "render second paragraph" in:
      doc
        .mainContent
        .selectOrFail("p.govuk-body")
        .get(1)
        .text() shouldBe "Your new agent services account will then be linked to those sign in details."

    "have the correct h2 heading" in:
      doc
        .mainContent
        .selectOrFail("h2")
        .get(1)
        .text() shouldBe "If you have more than one set of sign in details"

    "render third paragraph" in:
      doc
        .mainContent
        .selectOrFail("p.govuk-body")
        .get(2)
        .text() shouldBe "Choose the one you want to be linked to your new agent services account."

    "render a continue link styled as a button" in:
      doc.select("a.govuk-button[role=button]").text() shouldBe "Continue"
