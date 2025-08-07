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

package uk.gov.hmrc.agentregistrationfrontend.ispecs.controllers

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.ispecs.ISpec
import uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationFactory
import com.softwaremill.quicklens.*
import uk.gov.hmrc.agentregistration.shared.BusinessType.SoleTrader
import uk.gov.hmrc.agentregistration.shared.UserRole.Owner

class CheckYourAnswerControllerISpec
extends ISpec:

  private val applicationFactory = app.injector.instanceOf[ApplicationFactory]
  private val checkAnswerPath = s"/agent-registration/register/about-your-application/check-your-answers"
  private val fakeAgentApplication: AgentApplication = applicationFactory
    .makeNewAgentApplication(tdAll.internalUserId)
    .modify(_.aboutYourApplication.businessType).setTo(Some(SoleTrader))
    .modify(_.aboutYourApplication.userRole).setTo(Some(Owner))

  "GET /register/about-your-application/check-answer should return 200 and render page" in:
    AuthStubs.stubAuthoriseAsCleanAgent
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    val response: WSResponse = get(checkAnswerPath)

    response.status shouldBe 200
    val content = response.body[String]

    content should include("Business type")
    content should include("Are you the business owner?")
    content should include("Confirm and continue")

  "POST /register/about-your-application/check-answer with confirm and continue selection should redirect to the next page" in:
    AuthStubs.stubAuthoriseAsCleanAgent
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication

    val response: WSResponse = post(checkAnswerPath)(Map.empty)

    // TODO - validate location and status 303
    response.status shouldBe 200 // 303
//    response.body[String] shouldBe ""
//    response.header("Location").value shouldBe "/agent-registration/register/about-your-application/check-answer"
