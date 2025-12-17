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
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.forms.MemberNinoForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationMemberProvidedDetailsStubs

class MemberNinoControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/provide-details/nino"

  private object memberProvideDetails:

    private val afterEmailUpdate: MemberProvidedDetails = tdAll.providedDetailsLlp.afterEmailAddressProvided
    val beforeNinoMissingInHmrcSystems: MemberProvidedDetails = tdAll.providedDetailsLlp.withNinoNotProvided(afterEmailUpdate)
    val beforeNinoFromAuth: MemberProvidedDetails = tdAll.providedDetailsLlp.withNinoFromAuth(afterEmailUpdate)

    val afterNinoFromAuth: MemberProvidedDetails = beforeNinoFromAuth
    val afterNinoProvided: MemberProvidedDetails = tdAll.providedDetailsLlp.withNinoProvided(afterEmailUpdate)
    val afterNinoNotProvided: MemberProvidedDetails = tdAll.providedDetailsLlp.withNinoNotProvided(afterEmailUpdate)

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.MemberNinoController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.MemberNinoController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.MemberNinoController.submit.url shouldBe AppRoutes.providedetails.MemberNinoController.show.url

  s"GET $path should return 200 and render page when Nino is not provided in HMRC systems" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.beforeNinoMissingInHmrcSystems))
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Do you have a National Insurance number? - Apply for an agent services account - GOV.UK"

  s"GET $path should redirect to next page when Nino is already provided from HMRC systems (Auth)" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.beforeNinoFromAuth))
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"GET $path should redirect to previous page when EmailAddress is not provided from HMRC systems (Auth)" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.beforeNinoFromAuth.copy(emailAddress = None)))
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe routes.MemberEmailAddressController.show.url

  s"POST $path with selected Yes and valid name should save data and redirect to check your answers" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.beforeNinoMissingInHmrcSystems))
    AgentRegistrationMemberProvidedDetailsStubs.stubUpsertMemberProvidedDetails(memberProvideDetails.afterNinoProvided)

    val response: WSResponse =
      post(path)(Map(
        MemberNinoForm.hasNinoKey -> Seq("Yes"),
        MemberNinoForm.ninoKey -> Seq(tdAll.ninoProvided.nino.value)
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"POST $path with selected No should save data and redirect to check your answers" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.beforeNinoMissingInHmrcSystems))
    AgentRegistrationMemberProvidedDetailsStubs.stubUpsertMemberProvidedDetails(memberProvideDetails.afterNinoNotProvided)

    val response: WSResponse =
      post(path)(Map(
        MemberNinoForm.hasNinoKey -> Seq("No")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"POST $path  without selecting and option should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.beforeNinoMissingInHmrcSystems))
    val response: WSResponse =
      post(path)(Map(
        MemberNinoForm.hasNinoKey -> Seq(Constants.EMPTY_STRING),
        MemberNinoForm.ninoKey -> Seq(Constants.EMPTY_STRING)
      ))

    response.status shouldBe Status.BAD_REQUEST

    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Do you have a National Insurance number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#memberNino\\.hasNino-error").text() shouldBe "Error: Select yes if you have a National Insurance number"

  s"POST $path with selected Yes and blank inputs should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.beforeNinoMissingInHmrcSystems))
    val response: WSResponse =
      post(path)(Map(
        MemberNinoForm.hasNinoKey -> Seq("Yes"),
        MemberNinoForm.ninoKey -> Seq(Constants.EMPTY_STRING)
      ))

    response.status shouldBe Status.BAD_REQUEST

    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Do you have a National Insurance number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#memberNino\\.nino-error").text() shouldBe "Error: Enter your National Insurance number"

  s"POST $path with selected Yes and invalid characters should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.beforeNinoMissingInHmrcSystems))
    val response: WSResponse =
      post(path)(Map(
        MemberNinoForm.hasNinoKey -> Seq("Yes"),
        MemberNinoForm.ninoKey -> Seq("[[)(*%")
      ))

    response.status shouldBe Status.BAD_REQUEST

    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Do you have a National Insurance number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#memberNino\\.nino-error").text() shouldBe "Error: Enter a National Insurance number in the correct format"
