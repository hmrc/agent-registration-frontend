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
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.DataWithApplication
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.RequestWithData4
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.agentApplication
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentTelephoneNumberForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.agentdetails.AgentTelephoneNumberPage

class AgentTelephoneNumberPageSpec
extends ViewSpec:

  val viewTemplate: AgentTelephoneNumberPage = app.injector.instanceOf[AgentTelephoneNumberPage]
  implicit val agentApplicationRequest: RequestWithData4[DataWithApplication] = tdAll.makeAgentApplicationRequest(
    agentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterBusinessNameProvided
  )
  val doc: Document = Jsoup.parse(viewTemplate(
    form = AgentTelephoneNumberForm.form,
    bprTelephoneNumber = Some(tdAll.bprPrimaryTelephoneNumber),
    agentApplication = agentApplicationRequest.agentApplication
  ).body)
  private val heading: String = "What telephone number should we use for your agent services account?"

  "AgentTelephoneNumberPage" should:

    "contain expected content" in:
      doc.mainContent shouldContainContent
        """
          |Agent services account details
          |What telephone number should we use for your agent services account?
          |(+44) 10794554342
          |This is the number you have given us.
          |(+44) 78714743399
          |This is the number HMRC has in your business record.
          |or
          |Something else
          |Enter the number you want to use
          |We will not send a code to confirm this number.
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
          tdAll.telephoneNumber.value -> tdAll.telephoneNumber.value,
          tdAll.bprPrimaryTelephoneNumber -> tdAll.bprPrimaryTelephoneNumber,
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
      val field = AgentTelephoneNumberForm.key
      val errorMessage = "Enter the telephone number for your agent services account"
      val formWithError = AgentTelephoneNumberForm.form
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(
          form = formWithError,
          bprTelephoneNumber = Some(tdAll.bprPrimaryTelephoneNumber),
          agentApplication = agentApplicationRequest.agentApplication
        ).body),
        heading = heading
      )
