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

package uk.gov.hmrc.agentregistrationfrontend.views.individual

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.NotAgentCredentialPage

class NotAgentCredentialPageSpec
extends ViewSpec:

  private val viewTemplate: NotAgentCredentialPage = app.injector.instanceOf[NotAgentCredentialPage]

  private val continueCall = Call("GET", "/agent-registration/sign-out-with-continue?continueUrl=sign-in-url")
  private val doc: Document = Jsoup.parse(viewTemplate(continueCall).body)
  private val heading: String = "You need to sign in with personal tax account details"

  "NotAgentCredential page" should:
    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "have the correct content" in:
      doc.mainContent shouldContainContent
        """
          |You signed in with agent account details. To continue, sign in with the details you created for your personal taxes.
          |Continue
          |""".stripMargin

    "have a continue link" in:
      doc.selectOrFail(".govuk-button").selectOnlyOneElementOrFail().selectAttrOrFail("href") shouldBe continueCall.url
