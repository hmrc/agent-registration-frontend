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
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualSaUtrForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs

class IndividualSaUtrControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/provide-details/utr"

  private object individualProvideDetails:

    val afterNinoProvided: IndividualProvidedDetails = tdAll.providedDetailsLlp.AfterNino.afterNinoProvided
    val afterSaUtrNotProvided: IndividualProvidedDetails = tdAll.providedDetailsLlp.AfterSaUtr.afterSaUtrNotProvided
    val afterSaUtrFromAuth: IndividualProvidedDetails = tdAll.providedDetailsLlp.AfterSaUtr.afterSaUtrFromAuth
    val afterSaUtrFromCitizenDetails: IndividualProvidedDetails = tdAll.providedDetailsLlp.AfterSaUtr.afterSaUtrFromCitizenDetails
    val afterSaUtrProvided: IndividualProvidedDetails = tdAll.providedDetailsLlp.AfterSaUtr.afterSaUtrProvided

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.IndividualSaUtrController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.IndividualSaUtrController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.IndividualSaUtrController.submit.url shouldBe AppRoutes.providedetails.IndividualSaUtrController.show.url

  s"GET $path should return 200 and render page when SaUtr is not provided in HMRC systems" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails.afterSaUtrNotProvided))
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Do you have a Self Assessment Unique Taxpayer Reference? - Apply for an agent services account - GOV.UK"

  s"GET $path should redirect to next page when SaUtr is already provided from HMRC systems (Auth)" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails.afterSaUtrFromAuth))
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show.url

  s"GET $path should redirect to previous page when Nino is not provided from HMRC systems (Auth)" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails.afterSaUtrFromAuth.copy(individualNino =
      None
    )))
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.IndividualNinoController.show.url

  s"POST $path with selected Yes and valid name should save data and redirect to check your answers" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails.afterNinoProvided))
    AgentRegistrationIndividualProvidedDetailsStubs.stubUpsertIndividualProvidedDetails(individualProvideDetails.afterSaUtrProvided)

    val response: WSResponse =
      post(path)(Map(
        IndividualSaUtrForm.hasSaUtrKey -> Seq("Yes"),
        IndividualSaUtrForm.saUtrKey -> Seq(tdAll.saUtrProvided.saUtr.value)
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show.url

  s"POST $path with selected No should save data and redirect to check your answers" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails.afterSaUtrNotProvided))
    AgentRegistrationIndividualProvidedDetailsStubs.stubUpsertIndividualProvidedDetails(individualProvideDetails.afterSaUtrNotProvided)

    val response: WSResponse =
      post(path)(Map(
        IndividualSaUtrForm.hasSaUtrKey -> Seq("No")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show.url

  s"POST $path  without selecting and option should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails.afterSaUtrNotProvided))
    val response: WSResponse =
      post(path)(Map(
        IndividualSaUtrForm.hasSaUtrKey -> Seq(Constants.EMPTY_STRING),
        IndividualSaUtrForm.saUtrKey -> Seq(Constants.EMPTY_STRING)
      ))

    response.status shouldBe Status.BAD_REQUEST

    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Do you have a Self Assessment Unique Taxpayer Reference? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#individualSaUtr\\.hasSaUtr-error").text() shouldBe "Error: Select yes if you have a Self Assessment Unique Taxpayer Reference"

  s"POST $path with selected Yes and blank inputs should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails.afterSaUtrNotProvided))
    val response: WSResponse =
      post(path)(Map(
        IndividualSaUtrForm.hasSaUtrKey -> Seq("Yes"),
        IndividualSaUtrForm.saUtrKey -> Seq(Constants.EMPTY_STRING)
      ))

    response.status shouldBe Status.BAD_REQUEST

    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Do you have a Self Assessment Unique Taxpayer Reference? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#individualSaUtr\\.saUtr-error").text() shouldBe "Error: Enter your Self Assessment Unique Taxpayer Reference"

  s"POST $path with selected Yes and invalid characters should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails.afterSaUtrNotProvided))
    val response: WSResponse =
      post(path)(Map(
        IndividualSaUtrForm.hasSaUtrKey -> Seq("Yes"),
        IndividualSaUtrForm.saUtrKey -> Seq("[[)(*%")
      ))

    response.status shouldBe Status.BAD_REQUEST

    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Do you have a Self Assessment Unique Taxpayer Reference? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#individualSaUtr\\.saUtr-error").text() shouldBe "Error: Enter a Self Assessment Unique Taxpayer Reference in the correct format"
