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

package uk.gov.hmrc.agentregistrationfrontend.views.apply

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.DataWithApplication
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.RequestWithData
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.agentApplication
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.DeclarationPage

class DeclarationPageSpec
extends ViewSpec:

  val viewTemplate: DeclarationPage = app.injector.instanceOf[DeclarationPage]
  given agentApplicationRequest: RequestWithData[DataWithApplication] = tdAll.makeAgentApplicationRequest(tdAll.agentApplicationLlp.afterDeclarationSubmitted)

  val doc: Document = Jsoup.parse(
    viewTemplate(
      entityName = "Test Company Name",
      agentApplication = agentApplicationRequest.agentApplication
    ).body
  )

  "DeclarationPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |Declaration
          |I am authorised by Test Company Name to apply for an agent services account with HMRC.
          |The information I have given in this application is accurate to the best of my knowledge.
          |I understand that applying for an HMRC agent services account with the intention to commit tax fraud is a crime.
          |I understand that HMRC can suspend an agent services account if the agent business or any individual officer working on its behalf, fails up uphold the HMRC standard for agents.
          |Accept and send
          |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "Declaration - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Declaration"

    "render an accept and send button" in:
      doc
        .mainContent
        .selectOrFail(s"form button[value='AcceptAndSend']")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Accept and send"
