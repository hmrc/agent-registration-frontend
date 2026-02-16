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
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualNinoForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class IndividualNinoControllerSpec
extends ControllerSpec:

  private val linkId = tdAll.linkId
  private val agentApplication =
    tdAll
      .agentApplicationLlp
      .afterContactDetailsComplete
  private val path = s"/agent-registration/provide-details/nino/${linkId.value}"

  private object individualProvideDetails:

    val afterEmailProvided: IndividualProvidedDetails = tdAll.providedDetails.afterEmailAddressVerified
    val afterDateOfBirthProvided: IndividualProvidedDetails = tdAll.providedDetails.AfterDateOfBirth.afterDateOfBirthProvided
    val afterNinoNotProvided: IndividualProvidedDetails = tdAll.providedDetails.AfterNino.afterNinoNotProvided
    val afterNinoFromAuth: IndividualProvidedDetails = tdAll.providedDetails.AfterNino.afterNinoFromAuth
    val afterNinoProvided: IndividualProvidedDetails = tdAll.providedDetails.AfterNino.afterNinoProvided

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.IndividualNinoController.show(linkId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.IndividualNinoController.submit(linkId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.IndividualNinoController.submit(linkId).url shouldBe AppRoutes.providedetails.IndividualNinoController.show(linkId).url

  s"GET $path should return 200 and render page when Nino is not provided in HMRC systems" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.afterDateOfBirthProvided
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Do you have a National Insurance number? - Apply for an agent services account - GOV.UK"

  s"GET $path should redirect to next page when Nino is already provided from HMRC systems (Auth)" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.afterNinoFromAuth
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url

  s"GET $path should redirect to previous page when EmailAddress is not provided from HMRC systems (Auth)" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.afterNinoFromAuth.copy(
        emailAddress = None
      )
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.IndividualEmailAddressController.show(linkId).url

  s"POST $path with selected Yes and valid name should save data and redirect to check your answers" in:
    ProvideDetailsStubHelper.stubAuthAndUpdateProvidedDetails(
      agentApplication = agentApplication,
      individualProvidedDetails = individualProvideDetails.afterDateOfBirthProvided,
      updatedIndividualProvidedDetails = individualProvideDetails.afterNinoProvided
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualNinoForm.hasNinoKey -> Seq("Yes"),
        IndividualNinoForm.ninoKey -> Seq(tdAll.ninoProvided.nino.value)
      ))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url

  s"POST $path with selected No should save data and redirect to check your answers" in:
    ProvideDetailsStubHelper.stubAuthAndUpdateProvidedDetails(
      agentApplication = agentApplication,
      individualProvidedDetails = individualProvideDetails.afterNinoNotProvided,
      updatedIndividualProvidedDetails = individualProvideDetails.afterNinoNotProvided
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualNinoForm.hasNinoKey -> Seq("No")
      ))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url

  s"POST $path  without selecting and option should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.afterNinoNotProvided
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualNinoForm.hasNinoKey -> Seq(Constants.EMPTY_STRING),
        IndividualNinoForm.ninoKey -> Seq(Constants.EMPTY_STRING)
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Do you have a National Insurance number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#individualNino\\.hasNino-error").text() shouldBe "Error: Select yes if you have a National Insurance number"

  s"POST $path with selected Yes and blank inputs should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.afterNinoNotProvided
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualNinoForm.hasNinoKey -> Seq("Yes"),
        IndividualNinoForm.ninoKey -> Seq(Constants.EMPTY_STRING)
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Do you have a National Insurance number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#individualNino\\.nino-error").text() shouldBe "Error: Enter your National Insurance number"

  s"POST $path with selected Yes and invalid characters should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.afterNinoNotProvided
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualNinoForm.hasNinoKey -> Seq("Yes"),
        IndividualNinoForm.ninoKey -> Seq("[[)(*%")
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Do you have a National Insurance number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#individualNino\\.nino-error").text() shouldBe "Error: Enter a National Insurance number in the correct format"
