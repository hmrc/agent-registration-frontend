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

package uk.gov.hmrc.agentregistrationfrontend.views.apply.entitycheckfailed

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.entitycheckfailed.CanNotConfirmIdentityPage

class CanNotConfirmIdentityPageSpec
extends ViewSpec:

  val viewTemplate: CanNotConfirmIdentityPage = app.injector.instanceOf[CanNotConfirmIdentityPage]

  val doc: Document = Jsoup.parse(
    viewTemplate().body
  )

  "CannotConfirmIdentity" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |Get in touch to confirm your details
          | We need more details to confirm who you are. Get in touch using the information on Contact HMRC (opens in new tab).
          |If you’ve entered the incorrect details, you can try again.
          |Try again
          |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "Get in touch to confirm your details - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Get in touch to confirm your details"

    "render a try again button" in:
      val button = doc
        .mainContent
        .selectOrFail("a.govuk-button")
        .selectOnlyOneElementOrFail()

      button.text() shouldBe "Try again"
      button.attr("href") shouldBe "/agent-registration/apply/internal/confirm-identity-check"
