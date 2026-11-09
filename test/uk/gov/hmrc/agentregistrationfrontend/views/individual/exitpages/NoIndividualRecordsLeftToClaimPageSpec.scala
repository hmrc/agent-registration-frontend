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

package uk.gov.hmrc.agentregistrationfrontend.views.individual.exitpages

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.exitpages.NoIndividualRecordsLeftToClaimPage

class NoIndividualRecordsLeftToClaimPageSpec
extends ViewSpec:

  val linkId: LinkId = tdAll.linkId
  private val viewTemplate: NoIndividualRecordsLeftToClaimPage = app.injector.instanceOf[NoIndividualRecordsLeftToClaimPage]

  private val doc: Document = Jsoup.parse(viewTemplate(linkId).body)
  private val heading: String = "You cannot add your details to the application"

  "NoIndividualRecordsLeftToClaimPage should have the correct title" in:
    doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

  "NoIndividualRecordsLeftToClaimPage should have the correct content" in:
    doc.mainContent shouldContainContent
      """
        |You cannot add your details to the application
        |Check if you have already added your details using a different HMRC account.
        |If you did not add your details using a different HMRC account, contact the person who sent you the link.
        |Finish and sign out
        |""".stripMargin

  "NoIndividualRecordsLeftToClaimPage should have the correct heading" in:
    doc.h1 shouldBe heading

  "NoIndividualRecordsLeftToClaimPage should have the correct finish and sign out link" in:
    behavesLikePageWithFinishedAndSignOutLink(doc, Some(linkId.value))
