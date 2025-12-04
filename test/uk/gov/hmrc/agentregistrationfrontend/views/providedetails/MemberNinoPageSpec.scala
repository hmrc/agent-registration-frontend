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

package uk.gov.hmrc.agentregistrationfrontend.views.providedetails

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.MemberProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.forms.MemberNinoForm
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.memberconfirmation.MemberNinoPage

class MemberNinoPageSpec
extends ViewSpec:

  val viewTemplate: MemberNinoPage = app.injector.instanceOf[MemberNinoPage]
  implicit val memberProvideDetailsRequest: MemberProvideDetailsRequest[AnyContent] = tdAll
    .makeProvideDetailsRequest(memberProvidedDetails = tdAll.memberProvidedDetails)
  val doc: Document = Jsoup.parse(viewTemplate(MemberNinoForm.form).body)
  private val heading: String = "Do you have a National Insurance number?"

  "MemberNinoPage" should:

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a radio button for each option" in:
      val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
        legend = heading,
        options = List(
          YesNo.Yes.toString -> "Yes",
          YesNo.No.toString -> "No"
        ),
        hint = None
      )
      doc.mainContent.extractRadioGroup() shouldBe expectedRadioGroup

  "render a save and continue button" in:
    doc
      .mainContent
      .selectOrFail(s"form button[value='${SaveAndContinue.toString}']")
      .selectOnlyOneElementOrFail()
      .text() shouldBe "Save and continue"

  "render a save and come back later button" in:
    doc
      .mainContent
      .selectOrFail(s"form button[value=${SaveAndComeBackLater.toString}]")
      .selectOnlyOneElementOrFail()
      .text() shouldBe "Save and come back later"

  "render the form error correctly when the form contains an error" in:
    val field = MemberNinoForm.ninoKey
    val errorMessage = "Enter your National Insurance number"
    val formWithError = MemberNinoForm.form
      .withError(field, errorMessage)
    behavesLikePageWithErrorHandling(
      field = field,
      errorMessage = errorMessage,
      errorDoc = Jsoup.parse(viewTemplate(formWithError).body),
      heading = heading
    )
