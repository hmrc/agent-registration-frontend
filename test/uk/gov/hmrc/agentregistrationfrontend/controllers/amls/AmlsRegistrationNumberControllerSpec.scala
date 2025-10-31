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

package uk.gov.hmrc.agentregistrationfrontend.controllers.amls

import com.softwaremill.quicklens.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistrationfrontend.controllers
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsRegistrationNumberForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class AmlsRegistrationNumberControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/anti-money-laundering/registration-number"

  "routes should have correct paths and methods" in:
    arf.amls.routes.AmlsRegistrationNumberController.show shouldBe Call(
      method = "GET",
      url = "/agent-registration/apply/anti-money-laundering/registration-number"
    )
    arf.amls.routes.AmlsRegistrationNumberController.submit shouldBe Call(
      method = "POST",
      url = "/agent-registration/apply/anti-money-laundering/registration-number"
    )
    arf.amls.routes.AmlsRegistrationNumberController.submit.url shouldBe arf.amls.routes.AmlsRegistrationNumberController.show.url

  private case class TestCaseForAmlsRegistrationNumber(
    agentApplicationAfterSupervisoryBodySelected: AgentApplication,
    amlsType: String,
    validInput: String,
    invalidInput: String,
    nextPage: String
  )

  List(
    TestCaseForAmlsRegistrationNumber(
      agentApplicationAfterSupervisoryBodySelected =
        tdAll
          .agentApplicationLlp
          .sectionAmls
          .whenSupervisorBodyIsHmrc
          .afterSupervisoryBodySelected,
      amlsType = "HMRC",
      validInput = "XAML00000123456", // when the supervisory body is HMRC, the registration number has a different format to non-HMRC bodies
      invalidInput = "123",
      nextPage = arf.amls.routes.CheckYourAnswersController.show.url
    ),
    TestCaseForAmlsRegistrationNumber(
      agentApplicationAfterSupervisoryBodySelected =
        tdAll
          .agentApplicationLlp
          .sectionAmls
          .whenSupervisorBodyIsNonHmrc
          .afterSupervisoryBodySelected,
      amlsType = "non-HMRC",
      validInput = "1234567890",
      invalidInput = ";</\\>",
      nextPage = arf.amls.routes.AmlsExpiryDateController.show.url
    )
  ).foreach: testCase =>
    s"GET $path should return 200 for ${testCase.amlsType} and render page" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubGetAgentApplication(testCase.agentApplicationAfterSupervisoryBodySelected)
      val response: WSResponse = get(path)

      response.status shouldBe 200
      val doc = response.parseBodyAsJsoupDocument
      doc.title() shouldBe "What is your registration number? - Apply for an agent services account - GOV.UK"

    s"POST $path with valid input for ${testCase.amlsType} should redirect to the next page" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubGetAgentApplication(testCase.agentApplicationAfterSupervisoryBodySelected)
      val updatedApplication = testCase.agentApplicationAfterSupervisoryBodySelected
        .modify(_.amlsDetails.each)
        .setTo(AmlsDetails(
          supervisoryBody = testCase.agentApplicationAfterSupervisoryBodySelected.getAmlsDetails.supervisoryBody,
          amlsRegistrationNumber = Some(AmlsRegistrationNumber(testCase.validInput))
        ))
      AgentRegistrationStubs.stubUpdateAgentApplication(updatedApplication)
      AgentRegistrationStubs.stubGetAgentApplication(updatedApplication)
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(testCase.validInput),
          "submit" -> Seq("SaveAndContinue")
        ))

      response.status shouldBe 303
      response.body[String] shouldBe ""
      response.header("Location").value shouldBe testCase.nextPage

    s"POST $path with save for later and valid input for ${testCase.amlsType} should redirect to the saved for later page" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubGetAgentApplication(testCase.agentApplicationAfterSupervisoryBodySelected)
      val updatedApplication = testCase.agentApplicationAfterSupervisoryBodySelected
        .modify(_.amlsDetails.each)
        .setTo(AmlsDetails(
          supervisoryBody = testCase.agentApplicationAfterSupervisoryBodySelected.getAmlsDetails.supervisoryBody,
          amlsRegistrationNumber = Some(AmlsRegistrationNumber(testCase.validInput))
        ))
      AgentRegistrationStubs.stubUpdateAgentApplication(updatedApplication)
      AgentRegistrationStubs.stubGetAgentApplication(updatedApplication)
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(testCase.validInput),
          "submit" -> Seq("SaveAndComeBackLater")
        ))

      response.status shouldBe 303
      response.body[String] shouldBe ""
      response.header("Location").value shouldBe arf.routes.SaveForLaterController.show.url

    s"POST $path as blank form for ${testCase.amlsType} should return 400" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubGetAgentApplication(testCase.agentApplicationAfterSupervisoryBodySelected)
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(""),
          "submit" -> Seq("SaveAndContinue")
        ))

      response.status shouldBe 400
      val content = response.body[String]
      content should include("There is a problem")
      content should include("Enter your registration number")

    s"POST $path as blank form and save for later for ${testCase.amlsType} should redirect to save for later page" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubGetAgentApplication(testCase.agentApplicationAfterSupervisoryBodySelected)
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(""),
          "submit" -> Seq("SaveAndComeBackLater")
        ))

      response.status shouldBe 303
      response.body[String] shouldBe ""
      response.header("Location").value shouldBe arf.routes.SaveForLaterController.show.url

    s"POST $path with an invalid value for ${testCase.amlsType} should return 400" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubGetAgentApplication(testCase.agentApplicationAfterSupervisoryBodySelected)
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(testCase.invalidInput),
          "submit" -> Seq("SaveAndContinue")
        ))

      response.status shouldBe 400
      val content = response.body[String]
      content should include("There is a problem")
      content should include("Enter your registration number in the correct format")

    s"POST $path with an invalid value and save for later for ${testCase.amlsType} should not save and redirect to save for later" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubGetAgentApplication(testCase.agentApplicationAfterSupervisoryBodySelected)
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(testCase.invalidInput),
          "submit" -> Seq("SaveAndComeBackLater")
        ))

      response.status shouldBe 303
      response.body[String] shouldBe ""
      response.header("Location").value shouldBe arf.routes.SaveForLaterController.show.url
