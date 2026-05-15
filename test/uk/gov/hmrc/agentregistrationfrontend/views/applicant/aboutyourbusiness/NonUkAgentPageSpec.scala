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
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.aboutyourbusiness.NonUkAgentPage

class NonUkAgentPageSpec
extends ViewSpec:

  val viewTemplate: NonUkAgentPage = app.injector.instanceOf[NonUkAgentPage]
  implicit val doc: Document = Jsoup.parse(viewTemplate().body)
  private val heading: String = "You need to apply in a different way if the business is based outside the UK"

  "OverseasExplanatoryPage" should:
    "have the correct title" in:

      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |About your business
          |You need to apply in a different way if the business is based outside the UK
          |You can still apply online, but the information we ask for is different because the agent business is not in the UK.
          |Start the application process for a business based outside the UK
          |"""
          .stripMargin

    "should contain a link to the appeals guidance" in:
      val hmrcStandardLink: TestLink =
        doc.mainContent
          .selectOrFail("a.govuk-link")
          .get(0)
          .toLink

      hmrcStandardLink shouldBe TestLink(
        text = "Start the application process for a business based outside the UK",
        href = "https://www.gov.uk/guidance/apply-for-an-agent-services-account-if-you-are-not-based-in-the-uk"
      )
