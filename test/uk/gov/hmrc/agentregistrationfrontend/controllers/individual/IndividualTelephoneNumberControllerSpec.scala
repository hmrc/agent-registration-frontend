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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualTelephoneNumberForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class IndividualTelephoneNumberControllerSpec
extends ControllerSpec:

  private val linkId = tdAll.linkId
  private val agentApplication =
    tdAll
      .agentApplicationLlp
      .afterContactDetailsComplete
  private val path = s"/agent-registration/provide-details/telephone-number/${linkId.value}"

  private object individualProvideDetails:

    val beforeTelephoneUpdate: IndividualProvidedDetails = tdAll.providedDetails.afterStarted
    val afterTelephoneNumberProvided: IndividualProvidedDetails = tdAll.providedDetails.afterTelephoneNumberProvided

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.IndividualTelephoneNumberController.show(linkId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.IndividualTelephoneNumberController.submit(linkId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.IndividualTelephoneNumberController.submit(linkId).url shouldBe AppRoutes.providedetails.IndividualTelephoneNumberController.show(
      linkId
    ).url

  s"GET $path should return 200 and render page" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.beforeTelephoneUpdate
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What is your telephone number? - Apply for an agent services account - GOV.UK"
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()

  s"POST $path with a valid number should save data and redirect to email address page" in:
    ProvideDetailsStubHelper.stubAuthAndUpdateProvidedDetails(
      agentApplication = agentApplication,
      individualProvidedDetails = individualProvideDetails.beforeTelephoneUpdate,
      updatedIndividualProvidedDetails = individualProvideDetails.afterTelephoneNumberProvided
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualTelephoneNumberForm.key -> Seq(tdAll.telephoneNumber.value)
      ))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url

  s"POST $path with blank inputs should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.beforeTelephoneUpdate
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualTelephoneNumberForm.key -> Seq("")
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your telephone number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#individualTelephoneNumber-error").text() shouldBe "Error: Enter the number we should call to speak to you about this application"
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()

  s"POST $path with invalid characters should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.beforeTelephoneUpdate
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualTelephoneNumberForm.key -> Seq("[[)(*%")
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your telephone number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#individualTelephoneNumber-error").text() shouldBe "Error: Enter a phone number, like 01632 960 001 or 07700 900 982"
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()

  s"POST $path with more than 24 characters should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.beforeTelephoneUpdate
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualTelephoneNumberForm.key -> Seq("2".repeat(25))
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your telephone number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#individualTelephoneNumber-error").text() shouldBe "Error: The phone number must be 24 characters or fewer"
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()
