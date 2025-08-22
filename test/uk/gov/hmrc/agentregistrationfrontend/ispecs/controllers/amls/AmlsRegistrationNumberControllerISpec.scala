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

package uk.gov.hmrc.agentregistrationfrontend.ispecs.controllers.amls

import com.softwaremill.quicklens.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistrationfrontend.ispecs.ISpec
import uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationFactory

class AmlsRegistrationNumberControllerISpec
extends ISpec:

  private val applicationFactory = app.injector.instanceOf[ApplicationFactory]
  private val pathUnderTest = "/agent-registration/register/anti-money-laundering/registration-number"
  // when the supervisory body is HMRC, the registration number has a different format to non-HMRC bodies
  private val fakeAgentApplicationWithHmrc: AgentApplication = applicationFactory
    .makeNewAgentApplication(tdAll.internalUserId)
    .modify(_.amlsDetails)
    .setTo(Some(AmlsDetails(
      supervisoryBody = "HMRC",
      amlsRegistrationNumber = None
    )))
  private val fakeAgentApplicationNonHmrc: AgentApplication = applicationFactory
    .makeNewAgentApplication(tdAll.internalUserId)
    .modify(_.amlsDetails)
    .setTo(Some(AmlsDetails(
      supervisoryBody = "FCA",
      amlsRegistrationNumber = None
    )))

  private case class TestCaseForAmlsRegistrationNumber(
    application: AgentApplication,
    amlsType: String,
    validInput: String,
    invalidInput: String
  )

  List(
    TestCaseForAmlsRegistrationNumber(
      application = fakeAgentApplicationWithHmrc,
      amlsType = "HMRC",
      validInput = "XAML00000123456",
      invalidInput = "123"
    ),
    TestCaseForAmlsRegistrationNumber(
      application = fakeAgentApplicationNonHmrc,
      amlsType = "non-HMRC",
      validInput = "1234567890",
      invalidInput = ";</\\>"
    )
  ).foreach { testCase =>
    s"GET $pathUnderTest should return 200 for ${testCase.amlsType} and render page" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubApplicationInProgress(testCase.application)
      val response: WSResponse = get(pathUnderTest)

      response.status shouldBe 200
      val content = response.body[String]
      content should include("What is your registration number?")
      content should include("Save and continue")

    s"POST $pathUnderTest with valid input for ${testCase.amlsType} should redirect to the next page" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubApplicationInProgress(testCase.application)
      val updatedApplication = testCase.application
        .modify(_.amlsDetails.each)
        .setTo(AmlsDetails(
          supervisoryBody = testCase.application.getAmlsDetails.supervisoryBody,
          amlsRegistrationNumber = Some(AmlsRegistrationNumber(testCase.validInput))
        ))
      AgentRegistrationStubs.stubUpdateAgentApplication(updatedApplication)
      AgentRegistrationStubs.stubApplicationInProgress(updatedApplication)
      val response: WSResponse =
        post(pathUnderTest)(Map(
          "amlsRegistrationNumber" -> Seq(testCase.validInput),
          "submit" -> Seq("SaveAndContinue")
        ))

      response.status shouldBe 303
      response.body[String] shouldBe ""
      response.header("Location").value shouldBe "routes.nextPageInTask.TODO"

    s"POST $pathUnderTest with save for later and valid input for ${testCase.amlsType} should redirect to the saved for later page" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubApplicationInProgress(testCase.application)
      val updatedApplication = testCase.application
        .modify(_.amlsDetails.each)
        .setTo(AmlsDetails(
          supervisoryBody = testCase.application.getAmlsDetails.supervisoryBody,
          amlsRegistrationNumber = Some(AmlsRegistrationNumber(testCase.validInput))
        ))
      AgentRegistrationStubs.stubUpdateAgentApplication(updatedApplication)
      AgentRegistrationStubs.stubApplicationInProgress(updatedApplication)
      val response: WSResponse =
        post(pathUnderTest)(Map(
          "amlsRegistrationNumber" -> Seq(testCase.validInput),
          "submit" -> Seq("SaveAndComeBackLater")
        ))

      response.status shouldBe 303
      response.body[String] shouldBe ""
      response.header("Location").value shouldBe "routes.saveAndComeBackLater.TODO"

    s"POST $pathUnderTest as blank form for ${testCase.amlsType} should return 400" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubApplicationInProgress(testCase.application)
      val response: WSResponse =
        post(pathUnderTest)(Map(
          "amlsRegistrationNumber" -> Seq(""),
          "submit" -> Seq("SaveAndContinue")
        ))

      response.status shouldBe 400
      val content = response.body[String]
      content should include("There is a problem")
      content should include("Enter your registration number")

    s"POST $pathUnderTest as blank form and save for later for ${testCase.amlsType} should return 400" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubApplicationInProgress(testCase.application)
      val response: WSResponse =
        post(pathUnderTest)(Map(
          "amlsRegistrationNumber" -> Seq(""),
          "submit" -> Seq("SaveAndComeBackLater")
        ))

      response.status shouldBe 400
      val content = response.body[String]
      content should include("There is a problem")
      content should include("Enter your registration number")

    s"POST $pathUnderTest with an invalid value for ${testCase.amlsType} should return 400" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubApplicationInProgress(testCase.application)
      val response: WSResponse =
        post(pathUnderTest)(Map(
          "amlsRegistrationNumber" -> Seq(testCase.invalidInput),
          "submit" -> Seq("SaveAndContinue")
        ))

      response.status shouldBe 400
      val content = response.body[String]
      content should include("There is a problem")
      content should include("Enter your registration number in the correct format")

    s"POST $pathUnderTest with an invalid value and save for later for ${testCase.amlsType} should return 400" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubApplicationInProgress(testCase.application)
      val response: WSResponse =
        post(pathUnderTest)(Map(
          "amlsRegistrationNumber" -> Seq(testCase.invalidInput),
          "submit" -> Seq("SaveAndComeBackLater")
        ))

      response.status shouldBe 400
      val content = response.body[String]
      content should include("There is a problem")
      content should include("Enter your registration number in the correct format")

  }
