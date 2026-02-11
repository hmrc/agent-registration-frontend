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

package uk.gov.hmrc.agentregistrationfrontend.views.individual

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.StartPage

class StartPageSpec
extends ViewSpec:

  val viewTemplate: StartPage = app.injector.instanceOf[StartPage]
  private val linkId = LinkId("test-link-id")

  val doc: Document = Jsoup.parse(
    viewTemplate(linkId).body
  )

  "ProvideDetailsStartPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |Sign in and confirm your details
          |You have been nominated as a relevant individual that can support an application to HMRC for an agent services account.
          |We need some information from you before we can process the application.
          |Why we need this information
          |We ask you to sign in so we can:
          |confirm who you are
          |carry out some checks on your tax and financial background
          |Use the right sign in details
          |We need you to sign in using sign in details you created for your personal taxes, not your business taxes.
          |If you do not have personal sign in details, you can create some.
          |Start
          |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "Sign in and confirm your details - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Sign in and confirm your details"

    "render a link to start providing details" in:
      val startLink: TestLink =
        doc
          .mainContent
          .selectOrFail(".govuk-button--start")
          .selectOnlyOneElementOrFail()
          .toLink

      startLink shouldBe TestLink(
        text = "Start",
        href = s"/agent-registration/provide-details/internal/initiate-member-provide-details/${linkId.value}"
      )
