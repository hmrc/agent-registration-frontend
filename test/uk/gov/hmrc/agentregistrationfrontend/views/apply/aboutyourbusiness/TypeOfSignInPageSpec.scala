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

package uk.gov.hmrc.agentregistrationfrontend.views.apply.aboutyourbusiness

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.forms.TypeOfSignInForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.TypeOfSignInPage

class TypeOfSignInPageSpec
extends ViewSpec:

  val viewTemplate: TypeOfSignInPage = app.injector.instanceOf[TypeOfSignInPage]
  implicit val doc: Document = Jsoup.parse(viewTemplate(TypeOfSignInForm.form).body)
  private val heading: String = "Do you have an HMRC online services for agents account?"

  "BusinessTypePage" should:

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a radio button for each option" in:
      val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
        legend = heading,
        options = List(
          "Yes" -> "HmrcOnlineServices",
          "No, I do not manage any taxes for clients" -> "CreateSignInDetails"
        ),
        hint = None
      )
      doc.mainContent.extractRadioGroup() shouldBe expectedRadioGroup

    "render a continue button" in:
      doc.select("button[type=submit]").text() shouldBe "Continue"

    "render a form error when the form contains an error" in:
      val field = TypeOfSignInForm.key
      val errorMessage = "Select yes if you have an HMRC online services for agents account"
      val formWithError = TypeOfSignInForm.form
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(formWithError).body),
        heading = heading
      )
