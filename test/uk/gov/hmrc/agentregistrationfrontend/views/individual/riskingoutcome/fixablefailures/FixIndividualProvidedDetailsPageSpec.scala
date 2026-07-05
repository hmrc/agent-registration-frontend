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
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingoutcome.fixablefailures.FixIndividualProvidedDetailsPage

class FixIndividualProvidedDetailsPageSpec
extends ViewSpec:

  val viewTemplate: FixIndividualProvidedDetailsPage = app.injector.instanceOf[FixIndividualProvidedDetailsPage]

  val doc: Document = Jsoup.parse(
    viewTemplate(
      failureCode = "IndividualFix.10.IndividualDetailsFix",
      correctiveActionExpiryDate = "3 August 2026",
      linkId = tdAll.linkId
    ).body
  )

  "FixIndividualProvidedDetailsPage for individual" should:
    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |Provide your details
           |We could not match the details you provided with an HMRC record
           |This is usually because an answer you gave was incorrect, or you did not provide enough details.
           |On the next page, you can review some of your answers.
           |Check them, and change anything that is incorrect or missing.
           |You need to do this by 3 August 2026.
           |Continue
           |Is this page not working properly? (opens in new tab)
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "We could not match the details you provided with an HMRC record - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "We could not match the details you provided with an HMRC record"

    "should contain a link to the CYA page for identifiers" in:
      val detailsCyaLink: TestLink =
        doc
          .mainContent
          .selectOrFail("a.govuk-button")
          .get(0)
          .toLink

      detailsCyaLink shouldBe TestLink(
        text = "Continue",
        href = AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.CheckYourAnswersController.show(tdAll.linkId).url
      )
