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
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualDateOfBirthForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class IndividualDateOfBirthControllerSpec
extends ControllerSpec:

  private val linkId = tdAll.linkId
  private val agentApplication =
    tdAll
      .agentApplicationLlp
      .afterContactDetailsComplete
  private val path = s"/agent-registration/provide-details/date-of-birth/${linkId.value}"

  private object individualProvideDetails:

    val beforeEmailProvided: IndividualProvidedDetails = tdAll.providedDetails.afterTelephoneNumberProvided
    val afterEmailProvided: IndividualProvidedDetails = tdAll.providedDetails.afterEmailAddressVerified
    val afterDateOfBirthProvided: IndividualProvidedDetails = tdAll.providedDetails.AfterDateOfBirth.afterDateOfBirthProvided
    val afterDateOfBirthFromCitizensDetails: IndividualProvidedDetails = tdAll.providedDetails.AfterDateOfBirth.afterDateOfBirthFromCitizenDetails

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.IndividualDateOfBirthController.show(linkId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.IndividualDateOfBirthController.submit(linkId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.IndividualDateOfBirthController.submit(linkId).url shouldBe AppRoutes.providedetails.IndividualDateOfBirthController.show(
      linkId
    ).url

  s"GET $path should return 200 and render page when Date of Birth is not provided in HMRC systems" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.afterEmailProvided
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What is your date of birth? - Apply for an agent services account - GOV.UK"

  s"GET $path should redirect to next page when Date of Birth is already provided from HMRC systems (Citizens Details)" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.afterDateOfBirthFromCitizensDetails
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url

  s"GET $path should redirect to previous page when EmailAddress is not provided" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.beforeEmailProvided
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.IndividualEmailAddressController.show(linkId).url

  s"POST $path with valid date of birth should save data and redirect to check your answers" in:
    ProvideDetailsStubHelper.stubAuthAndUpdateProvidedDetails(
      agentApplication = agentApplication,
      individualProvidedDetails = individualProvideDetails.afterEmailProvided,
      updatedIndividualProvidedDetails = individualProvideDetails.afterDateOfBirthProvided
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualDateOfBirthForm.dayKey -> Seq(tdAll.dateOfBirth.getDayOfMonth.toString),
        IndividualDateOfBirthForm.monthKey -> Seq(tdAll.dateOfBirth.getMonthValue.toString),
        IndividualDateOfBirthForm.yearKey -> Seq(tdAll.dateOfBirth.getYear.toString)
      ))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url

  s"POST $path  without selecting and option should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.afterEmailProvided
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualDateOfBirthForm.dayKey -> Seq(Constants.EMPTY_STRING),
        IndividualDateOfBirthForm.monthKey -> Seq(Constants.EMPTY_STRING),
        IndividualDateOfBirthForm.yearKey -> Seq(Constants.EMPTY_STRING)
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your date of birth? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${IndividualDateOfBirthForm.key}-error"
    ).text() shouldBe "Error: Enter your date of birth"

  s"POST $path with invalid characters should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.afterEmailProvided
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualDateOfBirthForm.dayKey -> Seq("_"),
        IndividualDateOfBirthForm.monthKey -> Seq("_"),
        IndividualDateOfBirthForm.yearKey -> Seq("_")
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your date of birth? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${IndividualDateOfBirthForm.key}-error"
    ).text() shouldBe "Error: Your date of birth must be a real date"
