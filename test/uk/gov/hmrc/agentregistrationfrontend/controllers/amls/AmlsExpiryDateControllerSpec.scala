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

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsExpiryDateForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class AmlsExpiryDateControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/anti-money-laundering/supervision-runs-out"

  "routes should have correct paths and methods" in:
    routes.AmlsExpiryDateController.show shouldBe Call(
      method = "GET",
      url = path
    )
    routes.AmlsExpiryDateController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    routes.AmlsExpiryDateController.submit.url shouldBe routes.AmlsExpiryDateController.show.url

  private object agentApplication:

    val hmrcAmls: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsHmrc
        .afterRegistrationNumberProvided

    val beforeRegistrationNumberProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsHmrc
        .afterSupervisoryBodySelected

    val afterRegistrationNumberProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterRegistrationNumberProvided

    val afterAmlsExpiryDateProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterAmlsExpiryDateProvided

  s"GET $path should return 200 render page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterRegistrationNumberProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe "When does your supervision run out? - Apply for an agent services account - GOV.UK"

  s"GET $path when expiry date already stored should return 200 and render page with previous answer filled in" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterAmlsExpiryDateProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title shouldBe "When does your supervision run out? - Apply for an agent services account - GOV.UK"
    doc.select(s"input[name='${AmlsExpiryDateForm.dayKey}']")
      .attr("value") shouldBe agentApplication.afterAmlsExpiryDateProvided
      .getAmlsDetails
      .getAmlsExpiryDate
      .getDayOfMonth.toString
    doc.select(s"input[name='${AmlsExpiryDateForm.monthKey}']")
      .attr("value") shouldBe agentApplication.afterAmlsExpiryDateProvided
      .getAmlsDetails
      .getAmlsExpiryDate
      .getMonthValue.toString
    doc.select(s"input[name='${AmlsExpiryDateForm.yearKey}']")
      .attr("value") shouldBe agentApplication.afterAmlsExpiryDateProvided
      .getAmlsDetails
      .getAmlsExpiryDate
      .getYear.toString

  s"GET $path when registration number is missing should redirect to registration number page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeRegistrationNumberProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.AmlsRegistrationNumberController.show.url

  s"GET $path when supervisor is HMRC should redirect to check your answers" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.hmrcAmls)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"POST $path with valid inputs should redirect to the next page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterRegistrationNumberProvided)
    val updatedApplication: AgentApplicationLlp = agentApplication.afterAmlsExpiryDateProvided

    AgentRegistrationStubs.stubUpdateAgentApplication(updatedApplication)
    AgentRegistrationStubs.stubGetAgentApplication(updatedApplication)
    val response: WSResponse =
      post(path)(Map(
        AmlsExpiryDateForm.dayKey -> Seq(tdAll.amlsExpiryDateValid.getDayOfMonth.toString),
        AmlsExpiryDateForm.monthKey -> Seq(tdAll.amlsExpiryDateValid.getMonthValue.toString),
        AmlsExpiryDateForm.yearKey -> Seq(tdAll.amlsExpiryDateValid.getYear.toString),
        "submit" -> Seq("SaveAndContinue")
      ))

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"POST $path with save for later and valid input should redirect to the saved for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterRegistrationNumberProvided)
    val updatedApplication = agentApplication.afterAmlsExpiryDateProvided
    AgentRegistrationStubs.stubUpdateAgentApplication(updatedApplication)
    AgentRegistrationStubs.stubGetAgentApplication(updatedApplication)
    val response: WSResponse =
      post(path)(Map(
        AmlsExpiryDateForm.dayKey -> Seq(tdAll.amlsExpiryDateValid.getDayOfMonth.toString),
        AmlsExpiryDateForm.monthKey -> Seq(tdAll.amlsExpiryDateValid.getMonthValue.toString),
        AmlsExpiryDateForm.yearKey -> Seq(tdAll.amlsExpiryDateValid.getYear.toString),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applicationRoutes.SaveForLaterController.show.url

  s"POST $path as blank form should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterRegistrationNumberProvided)
    val response: WSResponse =
      post(path)(Map(
        AmlsExpiryDateForm.dayKey -> Seq(""),
        AmlsExpiryDateForm.monthKey -> Seq(""),
        AmlsExpiryDateForm.yearKey -> Seq(""),
        "submit" -> Seq("SaveAndContinue")
      ))

    response.status shouldBe 400
    response.parseBodyAsJsoupDocument.title shouldBe "Error: When does your supervision run out? - Apply for an agent services account - GOV.UK"

  s"POST $path as blank form and save for later should redirect to save for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterRegistrationNumberProvided)
    val response: WSResponse =
      post(path)(Map(
        AmlsExpiryDateForm.dayKey -> Seq(""),
        AmlsExpiryDateForm.monthKey -> Seq(""),
        AmlsExpiryDateForm.yearKey -> Seq(""),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applicationRoutes.SaveForLaterController.show.url

  s"POST $path with an invalid value should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterRegistrationNumberProvided)
    val response: WSResponse =
      post(path)(Map(
        AmlsExpiryDateForm.dayKey -> Seq(tdAll.amlsExpiryDateInvalid.getDayOfMonth.toString),
        AmlsExpiryDateForm.monthKey -> Seq(tdAll.amlsExpiryDateInvalid.getMonthValue.toString),
        AmlsExpiryDateForm.yearKey -> Seq(tdAll.amlsExpiryDateInvalid.getYear.toString),
        "submit" -> Seq("SaveAndContinue")
      ))

    response.status shouldBe 400
    response.parseBodyAsJsoupDocument.title shouldBe "Error: When does your supervision run out? - Apply for an agent services account - GOV.UK"

  s"POST $path with an invalid value and save for later should redirect to save for later" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterRegistrationNumberProvided)
    val response: WSResponse =
      post(path)(Map(
        AmlsExpiryDateForm.dayKey -> Seq(tdAll.amlsExpiryDateInvalid.getDayOfMonth.toString),
        AmlsExpiryDateForm.monthKey -> Seq(tdAll.amlsExpiryDateInvalid.getMonthValue.toString),
        AmlsExpiryDateForm.yearKey -> Seq(tdAll.amlsExpiryDateInvalid.getYear.toString),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applicationRoutes.SaveForLaterController.show.url
