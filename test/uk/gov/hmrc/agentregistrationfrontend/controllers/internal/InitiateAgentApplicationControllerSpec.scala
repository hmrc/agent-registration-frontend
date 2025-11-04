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

package uk.gov.hmrc.agentregistrationfrontend.controllers.internal

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.BusinessType.Partnership.LimitedLiabilityPartnership
import uk.gov.hmrc.agentregistrationfrontend.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.EnrolmentStoreStubs

class InitiateAgentApplicationControllerSpec
extends ControllerSpec:

  def initiateAgentApplication(
    agentType: AgentType,
    businessType: BusinessType
  ): String =
    val agentTypePathSegment =
      import uk.gov.hmrc.agentregistration.shared.util.EnumExtensions.toStringHyphenated
      agentType.toStringHyphenated
    val businessTypePathSegment =
      import uk.gov.hmrc.agentregistration.shared.util.SealedObjectsExtensions.toStringHyphenated
      businessType.toStringHyphenated
    s"/agent-registration/apply/internal/initiate-agent-application/$agentTypePathSegment/$businessTypePathSegment"

  final case class TestCase(
    agentType: AgentType,
    businessType: BusinessType
  )

  Seq(
    TestCase(AgentType.UkTaxAgent, LimitedLiabilityPartnership)
  ).foreach: t =>
    val initiateAgentApplicationUrl: String = initiateAgentApplication(agentType = t.agentType, businessType = t.businessType)
    s"routes should have correct paths and methods (${t.agentType}, ${t.businessType})" in:
      routes.InitiateAgentApplicationController.initiateAgentApplication(t.agentType, t.businessType) shouldBe Call(
        method = "GET",
        url = initiateAgentApplicationUrl
      )

    s"GET $initiateAgentApplicationUrl should create initial agent application" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubGetAgentApplicationNoContent()
      AgentRegistrationStubs.stubUpdateAgentApplication(tdAll.agentApplicationLlp.afterStarted)
      EnrolmentStoreStubs.stubQueryEnrolmentsAllocatedToGroup(
        tdAll.groupId,
        EnrolmentStoreProxyConnector.Enrolment(service = "HMRC-AS-AGENT", state = "Activated")
      )

      val response: WSResponse = get(initiateAgentApplicationUrl)
      response.status shouldBe Status.SEE_OTHER
      response.header("Location").value shouldBe routes.GrsController.startJourney().url
      AuthStubs.verifyAuthorise()
      AgentRegistrationStubs.verifyGetAgentApplication()
      AgentRegistrationStubs.verifyUpdateAgentApplication()
      EnrolmentStoreStubs.verifyQueryEnrolmentsAllocatedToGroup(tdAll.groupId)
