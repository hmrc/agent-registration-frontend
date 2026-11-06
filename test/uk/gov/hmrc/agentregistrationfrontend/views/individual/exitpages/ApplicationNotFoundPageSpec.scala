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
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.exitpages.ApplicationNotFoundPage

class ApplicationNotFoundPageSpec
extends ViewSpec:

  val linkId: LinkId = tdAll.linkId
  private val viewTemplate: ApplicationNotFoundPage = app.injector.instanceOf[ApplicationNotFoundPage]

  private val doc: Document = Jsoup.parse(viewTemplate(linkId).body)
  private val heading: String = "We cannot find an application for the link you entered"

  "ApplicationNotFoundPage should have the correct title" in:
    doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

  "ApplicationNotFoundPage should have the correct content" in:
    doc.mainContent shouldContainContent
      """
        |We cannot find an application for the link you entered
        |If you typed or pasted the link you were given, check it’s correct.
        |Contact the person who sent you the link, to check:
        |that they sent you the correct link
        |if the application has been deleted since they sent you the link
        |""".stripMargin

  "ApplicationNotFoundPage should have the correct heading" in:
    doc.h1 shouldBe heading
