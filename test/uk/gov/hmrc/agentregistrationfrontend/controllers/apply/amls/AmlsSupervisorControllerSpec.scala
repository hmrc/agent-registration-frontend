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
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp

import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsCodeForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class AmlsSupervisorControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/anti-money-laundering/supervisor-name"

  private object agentApplication:

    val baseForSectionAmls: AgentApplication = tdAll.agentApplicationLlp.baseForSectionAmls

    val afterSupervisoryBodySelected: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterSupervisoryBodySelected

    val afterHmrcSelected: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsHmrc
        .afterSupervisoryBodySelected

    val afterHmrcRegistrationNumberStored: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsHmrc
        .afterRegistrationNumberProvided

  "routes should have correct paths and methods" in:
    routes.AmlsSupervisorController.show shouldBe Call(
      method = "GET",
      url = "/agent-registration/apply/anti-money-laundering/supervisor-name"
    )
    routes.AmlsSupervisorController.submit shouldBe Call(
      method = "POST",
      url = "/agent-registration/apply/anti-money-laundering/supervisor-name"
    )
    routes.AmlsSupervisorController.submit.url shouldBe routes.AmlsSupervisorController.show.url

  s"GET $path should return 200 and render page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.baseForSectionAmls)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "What is the name of your supervisory body? - Apply for an agent services account - GOV.UK"

  s"GET $path when supervisory body already stored should return 200 and render page with previous answer selected" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterSupervisoryBodySelected)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "What is the name of your supervisory body? - Apply for an agent services account - GOV.UK"
    doc.select("select[name=amlsSupervisoryBody] option[selected]")
      .attr("value") shouldBe "ATT"

  s"POST $path with valid selection should redirect to the next page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.baseForSectionAmls)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterHmrcSelected)
    val response: WSResponse = post(path)(Map(AmlsCodeForm.key -> Seq("HMRC")))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"POST $path when changing value should unset registration number and redirect to the next page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterHmrcRegistrationNumberStored)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterSupervisoryBodySelected)
    val response: WSResponse = post(path)(Map(AmlsCodeForm.key -> Seq("ATT")))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"POST $path with save for later and valid selection should redirect to the saved for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.baseForSectionAmls)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterHmrcSelected)
    val response: WSResponse =
      post(path)(Map(
        AmlsCodeForm.key -> Seq("HMRC"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe CentralisedRoutes.apply.SaveForLaterController.show.url

  s"POST $path without valid selection should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.baseForSectionAmls)
    val response: WSResponse = post(path)(Map(AmlsCodeForm.key -> Seq("")))

    response.status shouldBe Status.BAD_REQUEST
    val content = response.body[String]
    content should include("There is a problem")
    content should include("Enter a name and choose your supervisor from the list")
