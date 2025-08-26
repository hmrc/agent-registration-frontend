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

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.ispecs.ISpec
import uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationFactory

class AmlsSupervisorControllerISpec
extends ISpec:

  private val applicationFactory = app.injector.instanceOf[ApplicationFactory]
  private val pathUnderTest = "/agent-registration/register/anti-money-laundering/supervisor-name"
  private val fakeAgentApplication: AgentApplication = applicationFactory.makeNewAgentApplication(tdAll.internalUserId)

  s"GET $pathUnderTest should return 200 and render page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    val response: WSResponse = get(pathUnderTest)

    response.status shouldBe 200
    val content = response.body[String]
    content should include("What is the name of your supervisory body?")
    content should include("Save and continue")

  s"POST $pathUnderTest with valid selection should redirect to the next page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication
    val response: WSResponse = post(pathUnderTest)(Map("amlsSupervisoryBody" -> Seq("HMRC")))

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "/agent-registration/register/anti-money-laundering/registration-number"

  s"POST $pathUnderTest with save for later and valid selection should redirect to the saved for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication
    val response: WSResponse =
      post(pathUnderTest)(Map(
        "amlsSupervisoryBody" -> Seq("HMRC"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "/agent-registration/register/save-and-come-back-later"

  s"POST $pathUnderTest without valid selection should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    val response: WSResponse = post(pathUnderTest)(Map("amlsSupervisoryBody" -> Seq("")))

    response.status shouldBe 400
    val content = response.body[String]
    content should include("There is a problem")
    content should include("Enter a name and choose your supervisor from the list")
