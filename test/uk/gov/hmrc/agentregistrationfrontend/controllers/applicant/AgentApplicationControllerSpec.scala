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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class AgentApplicationControllerSpec
extends ControllerSpec:

  private val applyPath: String = "/agent-registration/apply"
  private val viewApplicationPath: String = "/agent-registration/view-application"
  private val applicationDeletedPath: String = "/agent-registration/application-deleted"

  object agentApplication:

    val submitted: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterDeclarationSubmitted

  "routes should have correct paths and methods" in:
    AppRoutes.apply.AgentApplicationController.startRegistration shouldBe Call(
      method = "GET",
      url = "/agent-registration/apply"
    )
    AppRoutes.apply.AgentApplicationController.applicationStatus shouldBe Call(
      method = "GET",
      url = "/agent-registration/application-status"
    )
    AppRoutes.apply.AgentApplicationController.viewSubmittedApplication shouldBe Call(
      method = "GET",
      url = "/agent-registration/view-application"
    )
    AppRoutes.apply.AgentApplicationController.applicationDeleted shouldBe Call(
      method = "GET",
      url = "/agent-registration/application-deleted"
    )

  s"GET $applyPath should redirect to agent type page" in:
    val response: WSResponse = get(applyPath)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.aboutyourbusiness.AgentTypeController.show.url

  s"GET $viewApplicationPath should return OK" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.submitted)
    val response: WSResponse = get(viewApplicationPath)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Application for Test Company Name - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"GET $applicationDeletedPath should return OK when hasDeleted is in session and no application exists" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplicationNoContent()
    val response: WSResponse = get(
      uri = applicationDeletedPath,
      cookies = addHasDeletedApplicationToSession(hasDeleted = true).extractCookies
    )

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "We have deleted your application - Apply for an agent services account - GOV.UK"
    AuthStubs.verifyAuthorise()

  s"GET $applicationDeletedPath should redirect to landing page when hasDeleted is not in session" in:
    AuthStubs.stubAuthorise()
    val response: WSResponse = get(applicationDeletedPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.AgentApplicationController.landing.url
    AuthStubs.verifyAuthorise()

  s"GET $applicationDeletedPath should redirect to landing page when session says hasDeleted but an application still exists" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.submitted)
    val response: WSResponse = get(
      uri = applicationDeletedPath,
      cookies = addHasDeletedApplicationToSession(hasDeleted = true).extractCookies
    )

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.AgentApplicationController.landing.url
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
