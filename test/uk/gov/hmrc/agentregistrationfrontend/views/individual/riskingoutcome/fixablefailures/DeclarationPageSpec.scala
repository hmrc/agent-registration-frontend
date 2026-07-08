/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.views.individual.riskingoutcome.fixablefailures

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingoutcome.fixablefailures.DeclarationPage

class DeclarationPageSpec
extends ViewSpec:

  val viewTemplate: DeclarationPage = app.injector.instanceOf[DeclarationPage]

  val doc: Document = Jsoup.parse(
    viewTemplate(
      linkId = tdAll.linkId
    ).body
  )

  "DeclarationPage for individual fixes" should:
    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |Confirm your responses are final
           |You are about to submit your response
           |Submitting your responses means you believe you have done everything to meet the registration conditions.
           |Accept and send
           |Is this page not working properly? (opens in new tab)
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "You are about to submit your response - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "You are about to submit your response"

    "should contain a form to submit confirmation of the declaration" in:
      val form: Element = doc.select("form").selectOnlyOneElementOrFail()
      form.attr("action") shouldBe AppRoutes.providedetails.riskingoutcome.fixablefailures.DeclarationController.submit(tdAll.linkId).url
      form.attr("method") shouldBe ("POST")
      val submitButton: Element = form
        .selectOrFail("button[type=submit]")
        .selectOnlyOneElementOrFail()
      submitButton.text shouldBe "Accept and send"
