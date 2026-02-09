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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.internal

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
    businessType: BusinessType,
    userRole: UserRole
  ): String =
    val agentTypePathSegment =
      import uk.gov.hmrc.agentregistration.shared.util.EnumExtensions.toStringHyphenated
      agentType.toStringHyphenated
    val businessTypePathSegment =
      import uk.gov.hmrc.agentregistration.shared.util.SealedObjectsExtensions.toStringHyphenated
      businessType.toStringHyphenated
    val userRoleSegment =
      import uk.gov.hmrc.agentregistration.shared.util.EnumExtensions.toStringHyphenated
      userRole.toStringHyphenated
    s"/agent-registration/apply/internal/initiate-agent-application/$agentTypePathSegment/$businessTypePathSegment/$userRoleSegment"

  final case class TestCase(
    agentType: AgentType,
    businessType: BusinessType,
    userRole: UserRole
  )

  Seq(
    TestCase(
      AgentType.UkTaxAgent,
      LimitedLiabilityPartnership,
      UserRole.Authorised
    )
  ).foreach: t =>
    val initiateAgentApplicationUrl: String = initiateAgentApplication(
      agentType = t.agentType,
      businessType = t.businessType,
      userRole = t.userRole
    )
    s"routes should have correct paths and methods (${t.agentType}, ${t.businessType}, ${t.userRole})" in:
      AppRoutes.apply.internal.InitiateAgentApplicationController.initiateAgentApplication(
        t.agentType,
        t.businessType,
        t.userRole
      ) shouldBe Call(
        method = "GET",
        url = initiateAgentApplicationUrl
      )

    s"GET $initiateAgentApplicationUrl should create initial agent application" in:
      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubGetAgentApplicationNoContent()
      AgentRegistrationStubs.stubUpdateAgentApplication(tdAll.agentApplicationLlp.afterStarted)
      EnrolmentStoreStubs.stubQueryEnrolmentsAllocatedToGroupNoContent(tdAll.groupId)

      val response: WSResponse = get(initiateAgentApplicationUrl)
      response.status shouldBe Status.SEE_OTHER
      response.header("Location").value shouldBe AppRoutes.apply.internal.GrsController.startJourney().url
      AuthStubs.verifyAuthorise()
      AgentRegistrationStubs.verifyGetAgentApplication()
      AgentRegistrationStubs.verifyUpdateAgentApplication()
      EnrolmentStoreStubs.verifyQueryEnrolmentsAllocatedToGroup(tdAll.groupId)

    s"GET $initiateAgentApplicationUrl should redirect to taxAndSchemeManagementToSelfServeAssignmentOfAsaEnrolment when HmrcAsAgentEnrolment is Allocated to the group" in:
      AuthStubs.stubAuthorise()

      EnrolmentStoreStubs.stubQueryEnrolmentsAllocatedToGroup(
        tdAll.groupId,
        EnrolmentStoreProxyConnector.Enrolment(service = "HMRC-AS-AGENT", state = "Activated")
      )

      val response: WSResponse = get(initiateAgentApplicationUrl)
      response.status shouldBe Status.SEE_OTHER
      // TODO: actual url isn't known yet
      response.header("Location").value shouldBe "http://localhost:22201/taxAndSchemeManagementToSelfServeAssignmentOfAsaEnrolment"
      AuthStubs.verifyAuthorise()
      EnrolmentStoreStubs.verifyQueryEnrolmentsAllocatedToGroup(tdAll.groupId)
