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
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.CreateSignInDetailsPage

class CreateSignInDetailsPageSpec
extends ViewSpec:

  val viewTemplate: CreateSignInDetailsPage = app.injector.instanceOf[CreateSignInDetailsPage]

  val signInLink = uri"/agent-registration/sign-in"

  val doc: Document = Jsoup.parse(
    viewTemplate(signInLink).body
  )

  "SignInWithAgentDetailsPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |About your business
          |Create your agent account sign in details
          |You do not have agent sign in details yet.
          |You’ll need to create some before you can get an agent services account.
          |Select ‘Continue’ to start.
          |Continue
          |"""
          .stripMargin

    "have the correct caption" in:
      doc.selectOrFail(captionL).text() shouldBe "About your business"

    "have the correct title" in:
      doc.title() shouldBe "Create your agent account sign in details - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Create your agent account sign in details"

    "render first paragraph" in:
      doc
        .mainContent
        .selectOrFail("p.govuk-body")
        .first()
        .text() shouldBe "You do not have agent sign in details yet."

    "render second paragraph" in:
      doc
        .mainContent
        .selectOrFail("p.govuk-body")
        .get(1)
        .text() shouldBe "You’ll need to create some before you can get an agent services account."

    "render third paragraph" in:
      doc
        .mainContent
        .selectOrFail("p.govuk-body")
        .get(2)
        .text() shouldBe "Select ‘Continue’ to start."

    "render a continue link styled as a button" in:
      doc.select("a.govuk-button[role=button]").text() shouldBe "Continue"
