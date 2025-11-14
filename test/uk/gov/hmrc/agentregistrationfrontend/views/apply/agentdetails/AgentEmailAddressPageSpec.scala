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

package uk.gov.hmrc.agentregistrationfrontend.views.apply.agentdetails

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentEmailAddressForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.agentdetails.AgentEmailAddressPage

class AgentEmailAddressPageSpec
extends ViewSpec:

  val viewTemplate: AgentEmailAddressPage = app.injector.instanceOf[AgentEmailAddressPage]
  implicit val agentApplicationRequest: AgentApplicationRequest[AnyContent] = tdAll.makeAgentApplicationRequest(
    agentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterContactTelephoneSelected
  )
  val doc: Document = Jsoup.parse(viewTemplate(
    form = AgentEmailAddressForm.form,
    bprEmailAddress = Some(tdAll.bprEmailAddress)
  ).body)
  private val heading: String = "What email address should we use for your agent services account?"

  "AgentTelephoneNumberPage" should:

    "contain expected content" in:
      doc.mainContent shouldContainContent
        """
          |Agent services account details
          |What email address should we use for your agent services account?
          |user@test.com
          |This is the email address you have given us.
          |bpr@example.com
          |This is the email address HMRC has in your business record.
          |or
          |Something else
          |Enter the email address you want to use
          |We will send a code to this email address to confirm it.
          |Save and continue
          |Save and come back later
          |Is this page not working properly? (opens in new tab)
          |""".stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a radio button for each option" in:
      val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
        legend = heading,
        options = List(
          tdAll.applicantEmailAddress.value -> tdAll.applicantEmailAddress.value,
          tdAll.bprEmailAddress -> tdAll.bprEmailAddress,
          "Something else" -> "other"
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

    "render a form error when the form contains an error" in:
      val field = AgentEmailAddressForm.key
      val errorMessage = "Enter the email address for your agent services account"
      val formWithError = AgentEmailAddressForm.form
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(formWithError, Some(tdAll.bprEmailAddress)).body),
        heading = heading
      )
