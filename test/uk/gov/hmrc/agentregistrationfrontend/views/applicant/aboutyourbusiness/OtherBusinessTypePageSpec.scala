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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.aboutyourbusiness

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.aboutyourbusiness.OtherBusinessTypePage

class OtherBusinessTypePageSpec
extends ViewSpec:

  val viewTemplate: OtherBusinessTypePage = app.injector.instanceOf[OtherBusinessTypePage]
  private val signOutCall = Call("GET", "/agent-registration/sign-out")
  implicit val doc: Document = Jsoup.parse(viewTemplate().body)
  private val heading: String = "You cannot create an agent services account"

  "OtherBusinessTypePage" should:
    "have the correct title" in:

      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |You cannot create an agent services account
          |You can only create an agent services account if your business is one of the following:
          |a sole trader business
          |a limited company
          |a partnership
          |a limited liability partnership (LLP)
          |Finish and sign out
          |"""
          .stripMargin

    "have a finish and sign out link" in:
      doc.select(".govuk-button").text() shouldBe "Finish and sign out"
      doc.selectOrFail(".govuk-button").selectOnlyOneElementOrFail().selectAttrOrFail("href") shouldBe signOutCall.url
