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

package uk.gov.hmrc.agentregistrationfrontend.views.register

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentregistrationfrontend.forms.SelectFromOptionsForm
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessType
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpecSupport
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.BusinessTypePage

class BusinessTypePageSpec extends ViewSpecSupport {
  val viewTemplate: BusinessTypePage = app.injector.instanceOf[BusinessTypePage]
  implicit val doc: Document = Jsoup.parse(viewTemplate(SelectFromOptionsForm.form("businessType", BusinessType.names)).body)
  private val heading: String = "How is your business set up?"
  
  "BusinessTypePage" should {
    
    "have the correct title" in {
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"
    }

    "render a radio button for each option" in {
      val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
        legend = heading, 
        options = List(
          "Sole trader" -> "sole-trader",
          "Limited company" -> "limited-company",
          "Partnership" -> "general-partnership",
          "Limited liability partnership" -> "limited-liability-partnership"
        ),
        hint = None
      )
      doc.mainContent.extractRadios(1).value shouldBe expectedRadioGroup
    }
  }
}
