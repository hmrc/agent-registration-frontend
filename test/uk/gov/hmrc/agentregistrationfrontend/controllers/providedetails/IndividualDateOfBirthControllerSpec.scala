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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualDateOfBirthForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs

class IndividualDateOfBirthControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/provide-details/date-of-birth"

  private object individualProvideDetails:

    val beforeEmailProvided: IndividualProvidedDetailsToBeDeleted = tdAll.providedDetailsLlp.afterTelephoneNumberProvided
    val afterEmailProvided: IndividualProvidedDetailsToBeDeleted = tdAll.providedDetailsLlp.afterEmailAddressVerified
    val afterDateOfBirthProvided: IndividualProvidedDetailsToBeDeleted = tdAll.providedDetailsLlp.AfterDateOfBirth.afterDateOfBirthProvided
    val afterDateOfBirthFromCitizensDetails: IndividualProvidedDetailsToBeDeleted = tdAll.providedDetailsLlp.AfterDateOfBirth.afterDateOfBirthFromCitizenDetails

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.IndividualDateOfBirthController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.IndividualDateOfBirthController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.IndividualDateOfBirthController.submit.url shouldBe AppRoutes.providedetails.IndividualDateOfBirthController.show.url

  s"GET $path should return 200 and render page when Date of Birth is not provided in HMRC systems" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails.afterEmailProvided))
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What is your date of birth? - Apply for an agent services account - GOV.UK"

  s"GET $path should redirect to next page when Date of Birth is already provided from HMRC systems (Citizens Details)" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails.afterDateOfBirthFromCitizensDetails))
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show.url

  s"GET $path should redirect to previous page when EmailAddress is not provided" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails.beforeEmailProvided))
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.IndividualEmailAddressController.show.url

  s"POST $path with valid date of birth should save data and redirect to check your answers" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails.afterEmailProvided))
    AgentRegistrationIndividualProvidedDetailsStubs.stubUpsertIndividualProvidedDetails(individualProvideDetails.afterDateOfBirthProvided)

    val response: WSResponse =
      post(path)(Map(
        IndividualDateOfBirthForm.dayKey -> Seq(tdAll.dateOfBirth.getDayOfMonth.toString),
        IndividualDateOfBirthForm.monthKey -> Seq(tdAll.dateOfBirth.getMonthValue.toString),
        IndividualDateOfBirthForm.yearKey -> Seq(tdAll.dateOfBirth.getYear.toString)
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show.url

  s"POST $path  without selecting and option should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails.afterEmailProvided))
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
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails.afterEmailProvided))
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
