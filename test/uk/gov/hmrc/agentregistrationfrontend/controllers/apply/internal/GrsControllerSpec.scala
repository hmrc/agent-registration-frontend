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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.internal

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.routes as applyRoutes
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.GrsStubs

class GrsControllerSpec
extends ControllerSpec:

  private val journeyId: JourneyId = tdAll.grs.journeyId

  private val startJourneyPath: String = "/agent-registration/apply/internal/grs/start-journey"
  private val journeyCallbackPath: String = s"/agent-registration/apply/internal/grs/journey-callback?journeyId=${journeyId.value}"

  "routes should have correct paths and methods" in:
    routes.GrsController.startJourney() shouldBe Call(
      method = "GET",
      url = startJourneyPath
    )
    routes.GrsController.journeyCallback(Some(journeyId)) shouldBe Call(
      method = "GET",
      url = journeyCallbackPath
    )

  final case class TestCase(
    agentApplicationAfterStarted: AgentApplication,
    agentApplicationAfterGrsDataReceived: AgentApplication
  ):

    val businessType: BusinessType = agentApplicationAfterStarted.businessType
    agentApplicationAfterStarted.applicationState shouldBe ApplicationState.Started withClue "sanityCheck: this value makes sense only for the Started state"
    agentApplicationAfterGrsDataReceived.applicationState shouldBe ApplicationState.GrsDataReceived withClue "sanityCheck: this value makes sense only for the GrsDataReceived state"
    agentApplicationAfterStarted.businessType shouldBe agentApplicationAfterGrsDataReceived.businessType withClue "sanityCheck: business types must match"

  private val testCases: Seq[TestCase] = Seq(
    TestCase(
      agentApplicationAfterStarted = tdAll.agentApplicationLlp.afterStarted,
      agentApplicationAfterGrsDataReceived = tdAll.agentApplicationLlp.afterGrsDataReceived
    )
  )

  testCases.foreach: t =>
    s"GET $startJourneyPath should start start GRS journey and return Redirect to GRS (${t.businessType})" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubGetAgentApplication(t.agentApplicationAfterStarted)
      GrsStubs.stubCreateJourney(t.businessType)
      val response: WSResponse = get(startJourneyPath)
      response.status shouldBe Status.SEE_OTHER
      response.header("Location").value shouldBe GrsStubs.journeyStartRedirectUrl(t.businessType)
      AuthStubs.verifyAuthorise()
      AgentRegistrationStubs.verifyGetAgentApplication()
      GrsStubs.verifyCreateJourney(t.businessType)

    s"GET $journeyCallbackPath should retrieve JourneyData from GRS and redirect to the TaskList page (${t.businessType})" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubGetAgentApplication(t.agentApplicationAfterStarted)
      GrsStubs.stubGetJourneyData(
        t.businessType,
        journeyId,
        tdAll
      )
      AgentRegistrationStubs.stubUpdateAgentApplication(t.agentApplicationAfterGrsDataReceived)

      val response: WSResponse = get(journeyCallbackPath)
      response.status shouldBe Status.SEE_OTHER
      response.header("Location").value shouldBe applyRoutes.TaskListController.show.url
      AuthStubs.verifyAuthorise()
      AgentRegistrationStubs.verifyGetAgentApplication()
      GrsStubs.verifyGetJourneyData(t.businessType, journeyId)
      AgentRegistrationStubs.verifyUpdateAgentApplication()

  s"GET $startJourneyPath should skip GRS journey if GrsJourneyData are already retrieved and redirect to the TaskList page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(tdAll.agentApplicationLlp.afterGrsDataReceived)

    val response: WSResponse = get(startJourneyPath)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe applyRoutes.TaskListController.show.url

    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()

  s"GET $journeyCallbackPath should skip GRS journey if GrsJourneyData are already retrieved and redirect to the TaskList page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(tdAll.agentApplicationLlp.afterGrsDataReceived)

    val response: WSResponse = get(startJourneyPath)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe applyRoutes.TaskListController.show.url

    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
