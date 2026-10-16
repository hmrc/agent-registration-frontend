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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.ConfirmationOfDeletionPage

class ConfirmationOfDeletionPageSpec
extends ViewSpec:

  val viewTemplate: ConfirmationOfDeletionPage = app.injector.instanceOf[ConfirmationOfDeletionPage]

  val doc: Document = Jsoup.parse(
    viewTemplate().body
  )

  "ConfirmationOfDeletionPage should have expected content" in:
    doc.mainContent shouldContainContent
      s"""
         |We have deleted your application
         |What happens next
         |If you want to apply for an agent services account again, you can start a new application.
         |Finish and sign out
         |"""
        .stripMargin

  "ConfirmationOfDeletionPage should have the correct title" in:
    doc.title() shouldBe "We have deleted your application - Apply for an agent services account - GOV.UK"

  "ConfirmationOfDeletionPage should have the correct h1" in:
    doc.h1 shouldBe "We have deleted your application"

  "ConfirmationOfDeletionPage should contain a link to start again" in:
    val startAgainLink: TestLink =
      doc
        .mainContent
        .selectOrFail("a.govuk-link[href='/agent-registration/apply']")
        .selectOnlyOneElementOrFail()
        .toLink

    startAgainLink shouldBe TestLink(
      text = "start a new application",
      href = "/agent-registration/apply"
    )

  "ConfirmationOfDeletionPage should contain a link to finish and sign out" in:
    val startAgainLink: TestLink =
      doc
        .mainContent
        .selectOrFail(
          "a.govuk-link[href=/agent-registration/sign-out-with-continue?continueUrl=http%3A%2F%2Flocalhost%3A19001%2Fagent-registration%2Fsigned-out]"
        )
        .selectOnlyOneElementOrFail()
        .toLink

    startAgainLink shouldBe TestLink(
      text = "Finish and sign out",
      href = "/agent-registration/sign-out-with-continue?continueUrl=http%3A%2F%2Flocalhost%3A19001%2Fagent-registration%2Fsigned-out"
    )
