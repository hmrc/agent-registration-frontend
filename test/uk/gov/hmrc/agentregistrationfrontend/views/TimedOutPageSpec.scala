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

package uk.gov.hmrc.agentregistrationfrontend.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpecSupport
import uk.gov.hmrc.agentregistrationfrontend.views.html.TimedOutPage

class TimedOutPageSpec
extends ViewSpecSupport:

  val viewTemplate: TimedOutPage = app.injector.instanceOf[TimedOutPage]
  implicit val doc: Document = Jsoup.parse(viewTemplate().body)

  "TimedOutPage" should:

    "have the correct title" in:
      doc.title() shouldBe "You have been signed out - Apply for an agent services account - GOV.UK"

    "render explanation for sign out" in:
      doc.extractText("p.govuk-body", 1).get shouldBe
        "You have not done anything for 15 minutes, so we have signed you out to keep your account secure."

    "render a link to sign in again" in:
      doc.extractText("a.govuk-link", 1).get shouldBe "Sign in again"
