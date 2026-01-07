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

package uk.gov.hmrc.agentregistrationfrontend.views.apply.applicantcontactdetails

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.forms.TelephoneNumberForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.TelephoneNumberPage

class TelephoneNumberPageSpec
extends ViewSpec:

  val viewTemplate: TelephoneNumberPage = app.injector.instanceOf[TelephoneNumberPage]
  implicit val agentApplicationRequest: AgentApplicationRequest[AnyContent] = tdAll
    .makeAgentApplicationRequest(
      agentApplication =
        tdAll
          .agentApplicationLlp
          .sectionContactDetails
          .afterNameDeclared
    )
  val doc: Document = Jsoup.parse(viewTemplate(TelephoneNumberForm.form).body)
  private val heading: String = "If we need to speak to you about this application, what number do we call?"

  "TelephoneNumberPage" should:

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a form with an input of type tel for telephone number" in:
      val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe AppRoutes.apply.applicantcontactdetails.TelephoneNumberController.submit.url
      form
        .selectOrFail("label[for=telephoneNumber]")
        .selectOnlyOneElementOrFail()
        .text() shouldBe heading
      form
        .selectOrFail("input[name=telephoneNumber][type=tel]")
        .selectOnlyOneElementOrFail()

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
      val field = TelephoneNumberForm.key
      val errorMessage = "Enter the number we should call to speak to you about this application"
      val formWithError = TelephoneNumberForm.form
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(formWithError).body),
        heading = heading
      )
