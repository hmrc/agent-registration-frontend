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
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentCorrespondenceAddress
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.DataWithApplication
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.RequestWithData4
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentCorrespondenceAddressForm
import uk.gov.hmrc.agentregistrationfrontend.model.AddressOptions
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.agentdetails.AgentCorrespondenceAddressPage

class AgentCorrespondenceAddressPageSpec
extends ViewSpec:

  val viewTemplate: AgentCorrespondenceAddressPage = app.injector.instanceOf[AgentCorrespondenceAddressPage]
  implicit val agentApplicationRequest: RequestWithData4[DataWithApplication] = tdAll.makeAgentApplicationRequest(
    agentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterVerifiedEmailAddressSelected
  )
  private val addressOptions = AddressOptions(
    chroAddress = tdAll.companyProfile.unsanitisedCHROAddress,
    bprAddress = Some(tdAll.bprRegisteredAddress),
    otherAddress = None
  )
  private val addressOptionsWithOther = addressOptions.copy(
    otherAddress = Some(
      AgentCorrespondenceAddress(
        addressLine1 = tdAll.getConfirmedAddressResponse.lines.headOption.getOrThrowExpectedDataMissing("getConfirmedAddressResponse.line.head"),
        addressLine2 = tdAll.getConfirmedAddressResponse.lines.lift(1),
        addressLine3 = tdAll.getConfirmedAddressResponse.lines.lift(2),
        addressLine4 = tdAll.getConfirmedAddressResponse.lines.lift(3),
        postalCode = tdAll.getConfirmedAddressResponse.postcode,
        countryCode = tdAll.getConfirmedAddressResponse.country.code
      )
    )
  )
  val doc: Document = Jsoup.parse(viewTemplate(
    form = AgentCorrespondenceAddressForm.form,
    addressOptions = addressOptions
  ).body)
  val docWithOther: Document = Jsoup.parse(viewTemplate(
    form = AgentCorrespondenceAddressForm.form,
    addressOptions = addressOptionsWithOther
  ).body)
  private val heading: String = "What correspondence address should we use for your agent services account?"

  "AgentCorrespondenceAddressPage" should:

    "contain expected content when there is no other address" in:
      doc.mainContent shouldContainContent
        """
          |Agent services account details
          |What correspondence address should we use for your agent services account?
          |23 Great Portland Street, London, W1 8LT, GB
          |This is your Companies House registered office address.
          |Registered Line 1, Registered Line 2, AB1 2CD, GB
          |This is the address HMRC has in your business record.
          |or
          |Something else
          |Save and continue
          |Save and come back later
          |Is this page not working properly? (opens in new tab)
          |""".stripMargin

    "contain expected content when there is an other address" in:
      docWithOther.mainContent shouldContainContent
        """
          |Agent services account details
          |What correspondence address should we use for your agent services account?
          |23 Great Portland Street, London, W1 8LT, GB
          |This is your Companies House registered office address.
          |Registered Line 1, Registered Line 2, AB1 2CD, GB
          |This is the address HMRC has in your business record.
          |New Line 1, New Line 2, CD3 4EF, GB
          |This is the address you have given us.
          |or
          |Something else
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
          "23 Great Portland Street, London, W1 8LT, GB" -> "23 Great Portland Street, London, W1 8LT, GB",
          "Registered Line 1, Registered Line 2, AB1 2CD, GB" -> "Registered Line 1, Registered Line 2, AB1 2CD, GB",
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
      val field = AgentCorrespondenceAddressForm.key
      val errorMessage = "Enter the correspondence address you want to use on your agent services account"
      val formWithError = AgentCorrespondenceAddressForm.form
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(formWithError, addressOptions).body),
        heading = heading
      )
