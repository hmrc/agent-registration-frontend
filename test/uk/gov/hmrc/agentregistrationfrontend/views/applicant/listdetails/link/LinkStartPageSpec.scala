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
import uk.gov.hmrc.agentregistrationfrontend.util.DisplayDate
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.link.LinkStartPage

import java.time.LocalDate
import java.time.ZoneId

class LinkStartPageSpec
extends ViewSpec:

  val viewTemplate: LinkStartPage = app.injector.instanceOf[LinkStartPage]
  val applicationExpiryDate: LocalDate = tdAll.applicationExpiresAtAsInstant.atZone(ZoneId.systemDefault()).toLocalDate
  val applicationExpiryDateDisplay: String = DisplayDate.displayDateForLang(Some(applicationExpiryDate))
  val doc: Document = Jsoup.parse(
    viewTemplate(
      applicationExpiryDate = applicationExpiryDate
    ).body
  )

  "LinkPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |Ask all the relevant individuals to sign in
           |We need information from the people you told us about
           |You have created a list of people relevant to this application. We now need to hear from them.
           |Everyone you’ve told us about needs to sign in to confirm their identity.
           |We’ll give you a link to share with these people.
           |Important to know
           |We need everyone on the list to use the link to sign in by $applicationExpiryDateDisplay.
           |They should use sign in details for their personal taxes, not their business taxes. If they do not have personal sign in details, they can create some.
           |We’ll also ask them to provide some personal information, including:
           |name, date of birth, telephone number and email address
           |National Insurance number
           |Self Assessment Unique Taxpayer Reference
           |Continue
           |Save and come back later
           |Is this page not working properly? (opens in new tab)
           |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "We need information from the people you told us about - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "We need information from the people you told us about"
