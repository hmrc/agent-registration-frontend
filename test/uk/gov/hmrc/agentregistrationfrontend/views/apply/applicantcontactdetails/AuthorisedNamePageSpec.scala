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
import arf.applicantcontactdetails.routes
import uk.gov.hmrc.agentregistrationfrontend.forms.AuthorisedNameForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.AuthorisedNamePage

class AuthorisedNamePageSpec
extends ViewSpec:

  val viewTemplate: AuthorisedNamePage = app.injector.instanceOf[AuthorisedNamePage]
  implicit val agentApplicationRequest: AgentApplicationRequest[AnyContent] = tdAll.makeAgentApplicationRequest(
    agentApplication =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAuthorised
        .afterRoleSelected
  )
  val doc: Document = Jsoup.parse(viewTemplate(AuthorisedNameForm.form).body)
  private val heading: String = "What is your full name?"

  "MemberNamePage" should:

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a form with an input for full name" in:
      val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe routes.AuthorisedNameController.submit.url
      form
        .selectOrFail("label[for=authorisedName]")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "What is your full name?"
      form
        .selectOrFail("input[name=authorisedName][type=text]")
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

    "render a form error when the form contains an error on the name" in:
      val field = AuthorisedNameForm.key
      val errorMessage = "Enter your full name"
      val formWithError = AuthorisedNameForm.form
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(formWithError).body),
        heading = heading
      )
