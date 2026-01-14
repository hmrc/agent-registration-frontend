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
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.entitycheckfailed.CompanyStatusBlockPage

class CompanyStatusBlockPageSpec
extends ViewSpec:

  val viewTemplate: CompanyStatusBlockPage = app.injector.instanceOf[CompanyStatusBlockPage]

  val doc: Document = Jsoup.parse(
    viewTemplate().body
  )

  "CompanyStatusBlockPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |We cannot create an account for this company
          |We cannot register this company for an agent services account. This is because of the company's status on the Companies House register.
          |To view your company status you can search the Companies House register (opens in a new tab). If you believe the status is incorrect, contact Companies House.
          |If you've entered the incorrect details, you can try again.
          |Try again
          |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "We cannot create an account for this company - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "We cannot create an account for this company"

    "render a try again button" in:
      val button = doc
        .mainContent
        .selectOrFail("a.govuk-button")
        .selectOnlyOneElementOrFail()

      button.text() shouldBe "Try again"
      button.attr("href") shouldBe "/agent-registration/apply/internal/companies-house-status-check"
