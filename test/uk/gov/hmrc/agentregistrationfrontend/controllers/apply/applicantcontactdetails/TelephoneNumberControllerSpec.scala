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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.applicantcontactdetails

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.TelephoneNumberForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class TelephoneNumberControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/applicant/telephone-number"

  private object agentApplication:

    val beforeTelephoneUpdate: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .afterNameDeclared

    val afterTelephoneNumberProvided: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .afterTelephoneNumberProvided

  private object ExpectedStrings:

    private val heading = "If we need to speak to you about this application, what number do we call?"
    val title = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $heading - Apply for an agent services account - GOV.UK"
    val requiredError = "Enter the number we should call to speak to you about this application"
    val invalidError = "Enter a phone number, like 01632 960 001 or 07700 900 982"
    val maxLengthError = "The phone number must be 24 characters or fewer"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.applicantcontactdetails.TelephoneNumberController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.applicantcontactdetails.TelephoneNumberController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.applicantcontactdetails.TelephoneNumberController.submit.url shouldBe AppRoutes.apply.applicantcontactdetails.TelephoneNumberController.show.url

  s"GET $path should return 200 and render page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeTelephoneUpdate)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe ExpectedStrings.title
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with a valid number should save data and redirect to check your answers" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeTelephoneUpdate,
      updatedApplication = agentApplication.afterTelephoneNumberProvided
    )
    val response: WSResponse =
      post(path)(Map(
        TelephoneNumberForm.key -> Seq(tdAll.telephoneNumber.value)
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.applicantcontactdetails.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeTelephoneUpdate)
    val response: WSResponse =
      post(path)(Map(
        TelephoneNumberForm.key -> Seq(Constants.EMPTY_STRING)
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(s"#${TelephoneNumberForm.key}-error").text() shouldBe s"Error: ${ExpectedStrings.requiredError}"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with invalid characters should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeTelephoneUpdate)
    val response: WSResponse =
      post(path)(Map(
        TelephoneNumberForm.key -> Seq("[[)(*%")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(s"#${TelephoneNumberForm.key}-error").text() shouldBe s"Error: ${ExpectedStrings.invalidError}"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with more than 24 characters should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeTelephoneUpdate)
    val response: WSResponse =
      post(path)(Map(
        TelephoneNumberForm.key -> Seq("2".repeat(25))
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(s"#${TelephoneNumberForm.key}-error").text() shouldBe s"Error: ${ExpectedStrings.maxLengthError}"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with save for later and valid selection should save data and redirect to the saved for later page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeTelephoneUpdate,
      updatedApplication = agentApplication.afterTelephoneNumberProvided
    )
    val response: WSResponse =
      post(path)(Map(
        TelephoneNumberForm.key -> Seq(tdAll.telephoneNumber.value),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with save for later and invalid inputs should not return errors and redirect to save for later page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeTelephoneUpdate)
    val response: WSResponse =
      post(path)(Map(
        TelephoneNumberForm.key -> Seq("[[*%"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
