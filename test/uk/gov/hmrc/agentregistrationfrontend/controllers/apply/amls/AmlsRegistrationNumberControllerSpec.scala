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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.amls

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsRegistrationNumberForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class AmlsRegistrationNumberControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/anti-money-laundering/registration-number"

  private object agentApplication:

    val hmrcAfterSupervisoryBodySelected: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsHmrc
        .afterSupervisoryBodySelected

    val hmrcAfterRegistrationNumberProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsHmrc
        .afterRegistrationNumberProvided

    val nonHmrcAfterSupervisoryBodySelected: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterSupervisoryBodySelected

    val nonHmrcAfterRegistrationNumberProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterRegistrationNumberProvided

  private object ExpectedStrings:

    val heading = "What is your registration number?"
    val title = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $heading - Apply for an agent services account - GOV.UK"
    val hint = "This is the registration number given to you by your supervisory body."
    val requiredError = "Enter your registration number"
    val invalidFormatError = "Enter your registration number in the correct format"

  "routes should have correct paths and methods" in:
    routes.AmlsRegistrationNumberController.show shouldBe Call(
      method = "GET",
      url = "/agent-registration/apply/anti-money-laundering/registration-number"
    )
    routes.AmlsRegistrationNumberController.submit shouldBe Call(
      method = "POST",
      url = "/agent-registration/apply/anti-money-laundering/registration-number"
    )
    routes.AmlsRegistrationNumberController.submit.url shouldBe routes.AmlsRegistrationNumberController.show.url

  private case class TestCaseForAmlsRegistrationNumber(
    application: AgentApplicationLlp,
    updatedApplication: AgentApplicationLlp,
    amlsType: String,
    validInput: String,
    invalidInput: String,
    nextPage: String
  )

  List(
    TestCaseForAmlsRegistrationNumber(
      application = agentApplication.hmrcAfterSupervisoryBodySelected,
      updatedApplication = agentApplication.hmrcAfterRegistrationNumberProvided,
      amlsType = "HMRC",
      validInput = "XAML00000123456", // when the supervisory body is HMRC, the registration number has a different format to non-HMRC bodies
      invalidInput = "123",
      nextPage = routes.CheckYourAnswersController.show.url
    ),
    TestCaseForAmlsRegistrationNumber(
      application = agentApplication.nonHmrcAfterSupervisoryBodySelected,
      updatedApplication = agentApplication.nonHmrcAfterRegistrationNumberProvided,
      amlsType = "non-HMRC",
      validInput = "NONHMRC-REF-AMLS-NUMBER-00001",
      invalidInput = ";</\\>",
      nextPage = routes.CheckYourAnswersController.show.url
    )
  ).foreach: testCase =>
    s"GET $path should return 200 for ${testCase.amlsType} and render page" in:
      ApplyStubHelper.stubsForAuthAction(testCase.application)
      val response: WSResponse = get(path)

      response.status shouldBe Status.OK
      val doc = response.parseBodyAsJsoupDocument
      doc.title() shouldBe ExpectedStrings.title
      ApplyStubHelper.verifyConnectorsForAuthAction()

    s"POST $path with valid input for ${testCase.amlsType} should redirect to the next page" in:
      ApplyStubHelper.stubsForSuccessfulUpdate(
        application = testCase.application,
        updatedApplication = testCase.updatedApplication
      )
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(testCase.validInput),
          "submit" -> Seq("SaveAndContinue")
        ))

      response.status shouldBe Status.SEE_OTHER
      response.body[String] shouldBe Constants.EMPTY_STRING
      response.header("Location").value shouldBe testCase.nextPage
      ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

    s"POST $path with save for later and valid input for ${testCase.amlsType} should redirect to the saved for later page" in:
      ApplyStubHelper.stubsForSuccessfulUpdate(
        application = testCase.application,
        updatedApplication = testCase.updatedApplication
      )
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(testCase.validInput),
          "submit" -> Seq("SaveAndComeBackLater")
        ))

      response.status shouldBe Status.SEE_OTHER
      response.body[String] shouldBe Constants.EMPTY_STRING
      response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
      ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

    s"POST $path as blank form for ${testCase.amlsType} should return 400" in:
      ApplyStubHelper.stubsForAuthAction(testCase.application)
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(Constants.EMPTY_STRING),
          "submit" -> Seq("SaveAndContinue")
        ))

      response.status shouldBe Status.BAD_REQUEST
      val doc = response.parseBodyAsJsoupDocument
      doc.title shouldBe ExpectedStrings.errorTitle
      doc.mainContent.select(
        s"#${AmlsRegistrationNumberForm.key}-error"
      ).text() shouldBe s"Error: ${ExpectedStrings.requiredError}"
      ApplyStubHelper.verifyConnectorsForAuthAction()

    s"POST $path as blank form and save for later for ${testCase.amlsType} should redirect to save for later page" in:
      ApplyStubHelper.stubsForAuthAction(testCase.application)
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(Constants.EMPTY_STRING),
          "submit" -> Seq("SaveAndComeBackLater")
        ))

      response.status shouldBe Status.SEE_OTHER
      response.body[String] shouldBe Constants.EMPTY_STRING
      response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
      ApplyStubHelper.verifyConnectorsForAuthAction()

    s"POST $path with an invalid value for ${testCase.amlsType} should return 400" in:
      ApplyStubHelper.stubsForAuthAction(testCase.application)
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(testCase.invalidInput),
          "submit" -> Seq("SaveAndContinue")
        ))

      response.status shouldBe Status.BAD_REQUEST
      val doc = response.parseBodyAsJsoupDocument
      doc.title shouldBe ExpectedStrings.errorTitle
      doc.mainContent.select(
        s"#${AmlsRegistrationNumberForm.key}-error"
      ).text() shouldBe s"Error: ${ExpectedStrings.invalidFormatError}"
      ApplyStubHelper.verifyConnectorsForAuthAction()

    s"POST $path with an invalid value and save for later for ${testCase.amlsType} should not save and redirect to save for later" in:
      ApplyStubHelper.stubsForAuthAction(testCase.application)
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(testCase.invalidInput),
          "submit" -> Seq("SaveAndComeBackLater")
        ))

      response.status shouldBe Status.SEE_OTHER
      response.body[String] shouldBe Constants.EMPTY_STRING
      response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
      ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path when registration number already stored should return 200 and render page with previous answer filled in" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.nonHmrcAfterRegistrationNumberProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.title
    doc
      .select(s"input[name='${AmlsRegistrationNumberForm.key}']")
      .attr("value") shouldBe "NONHMRC-REF-AMLS-NUMBER-00001"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path when amls details are missing should redirect to supervisory body page" in:
    ApplyStubHelper.stubsForAuthAction(tdAll.agentApplicationLlp.afterGrsDataReceived)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe routes.AmlsSupervisorController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path when amls details are missing should redirect to supervisory body page" in:
    ApplyStubHelper.stubsForAuthAction(tdAll.agentApplicationLlp.afterGrsDataReceived)
    val response: WSResponse =
      post(path)(Map(
        AmlsRegistrationNumberForm.key -> Seq("1234567890")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe routes.AmlsSupervisorController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
