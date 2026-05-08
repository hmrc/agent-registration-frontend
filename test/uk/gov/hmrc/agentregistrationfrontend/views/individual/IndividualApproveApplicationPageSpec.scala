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
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.IndividualApproveApplicationPage

class IndividualApproveApplicationPageSpec
extends ViewSpec:

  val linkId: LinkId = tdAll.linkId
  val viewTemplate: IndividualApproveApplicationPage = app.injector.instanceOf[IndividualApproveApplicationPage]

  val doc: Document = Jsoup.parse(
    viewTemplate(
      "Test Officer",
      linkId
    ).body
  )

  "HmrcStandardForAgentsPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |Approve the applicant
          |You need to confirm that Test Officer was authorised to make this application.
          |Approve and continue
          |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "Approve the applicant - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Approve the applicant"

    "render an approve and continue button" in:
      doc
        .mainContent
        .selectOrFail(s"form button[value='ApproveAndContinue']")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Approve and continue"
