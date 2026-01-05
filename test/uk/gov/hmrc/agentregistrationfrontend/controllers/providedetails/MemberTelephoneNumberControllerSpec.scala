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
import uk.gov.hmrc.agentregistrationfrontend.forms.MemberTelephoneNumberForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationMemberProvidedDetailsStubs

class MemberTelephoneNumberControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/provide-details/telephone-number"

  private object memberProvideDetails:

    val beforeTelephoneUpdate: MemberProvidedDetails = tdAll.providedDetailsLlp.afterOfficerChosen

    val afterTelephoneNumberProvided: MemberProvidedDetails = tdAll.providedDetailsLlp.afterTelephoneNumberProvided

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.MemberTelephoneNumberController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.MemberTelephoneNumberController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.MemberTelephoneNumberController.submit.url shouldBe AppRoutes.providedetails.MemberTelephoneNumberController.show.url

  s"GET $path should return 200 and render page" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.beforeTelephoneUpdate))
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What is your telephone number? - Apply for an agent services account - GOV.UK"

  s"POST $path with a valid number should save data and redirect to email address page" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.beforeTelephoneUpdate))
    AgentRegistrationMemberProvidedDetailsStubs.stubUpsertMemberProvidedDetails(memberProvideDetails.afterTelephoneNumberProvided)

    val response: WSResponse =
      post(path)(Map(
        MemberTelephoneNumberForm.key -> Seq(tdAll.telephoneNumber.value)
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"POST $path with blank inputs should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.beforeTelephoneUpdate))
    val response: WSResponse =
      post(path)(Map(
        MemberTelephoneNumberForm.key -> Seq("")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your telephone number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#memberTelephoneNumber-error").text() shouldBe "Error: Enter the number we should call to speak to you about this application"

  s"POST $path with invalid characters should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.beforeTelephoneUpdate))
    val response: WSResponse =
      post(path)(Map(
        MemberTelephoneNumberForm.key -> Seq("[[)(*%")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your telephone number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#memberTelephoneNumber-error").text() shouldBe "Error: Enter a phone number, like 01632 960 001 or 07700 900 982"

  s"POST $path with more than 24 characters should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.beforeTelephoneUpdate))
    val response: WSResponse =
      post(path)(Map(
        MemberTelephoneNumberForm.key -> Seq("2".repeat(25))
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is your telephone number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#memberTelephoneNumber-error").text() shouldBe "Error: The phone number must be 24 characters or fewer"
