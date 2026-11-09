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
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.exitpages.NameNotFoundPage

class NameNotFoundPageSpec
extends ViewSpec:

  val linkId: LinkId = tdAll.linkId
  private val viewTemplate: NameNotFoundPage = app.injector.instanceOf[NameNotFoundPage]

  private val doc: Document = Jsoup.parse(viewTemplate(linkId).body)
  private val heading: String = "We cannot find your name in the application"

  "NameNotFoundPage should have the correct title" in:
    doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

  "NameNotFoundPage should have the correct content" in:
    doc.mainContent shouldContainContent
      """
        |We cannot find your name in the application
        |Contact the person who sent you the link, to check how they entered your name in the application.
        |Finish and sign out
        |""".stripMargin

  "NameNotFoundPage should have the correct heading" in:
    doc.h1 shouldBe heading

  "NameNotFoundPage should have the correct finish and sign out link" in:
    behavesLikePageWithFinishedAndSignOutLink(doc, Some(linkId.value))
