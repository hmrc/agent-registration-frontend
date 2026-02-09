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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.link

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.link.LinkPage

class LinkPageSpec
extends ViewSpec:

  val viewTemplate: LinkPage = app.injector.instanceOf[LinkPage]
  val doc: Document = Jsoup.parse(
    viewTemplate(
      agentApplication = tdAll.agentApplicationGeneralPartnership.afterHowManyKeyIndividuals,
      existingList = List(
        tdAll.individualProvidedDetails,
        tdAll.individualProvidedDetails2,
        tdAll.individualProvidedDetails3
      )
    ).body
  )

  "LinkPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |Partner and tax adviser information
           |Share this link with everyone on the list
           |The people on your list need to sign in and provide their details to HMRC.
           |Share this link with them:
           |$thisFrontendBaseUrl/agent-registration/provide-details/start/link-id-12345
           |Copy link to clipboard
           |Link copied
           |See the list of people again
           |This is the list of people you’ve told us about:
           |Test Name
           |Second Test Name
           |Third Test Name
           |Select ‘Confirm and continue’ when you have shared the link with everyone on the list.
           |Confirm and continue
           |Save and come back later
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "Share this link with everyone on the list - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Share this link with everyone on the list"
