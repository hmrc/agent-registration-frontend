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

package uk.gov.hmrc.agentregistrationfrontend.controllers

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationFactory
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class BusinessTypeControllerISpec
extends ISpec:

  private val applicationFactory = app.injector.instanceOf[ApplicationFactory]
  private val businessTypePath = "/agent-registration/register/about-your-application/business-type"
  private val fakeAgentApplication: AgentApplication = applicationFactory.makeNewAgentApplication(tdAll.internalUserId)

  "GET /register should redirect to business type page" in:
    val response: WSResponse = get("/agent-registration/register")

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe businessTypePath

  s"GET $businessTypePath should return 200 and render page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    val response: WSResponse = get(businessTypePath)

    response.status shouldBe 200
    val content = response.body[String]
    content should include("How is your business set up?")
    content should include("Save and continue")

  s"POST $businessTypePath with valid selection should redirect to the next page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication
    val response: WSResponse = post(businessTypePath)(Map("businessType" -> Seq("SoleTrader")))

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "/agent-registration/register/about-your-application/user-role"

  s"POST $businessTypePath without valid selection should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    val response: WSResponse = post(businessTypePath)(Map("businessType" -> Seq("")))

    response.status shouldBe 400
    val content = response.body[String]
    content should include("There is a problem")
    content should include("Tell us how your business is set up")
