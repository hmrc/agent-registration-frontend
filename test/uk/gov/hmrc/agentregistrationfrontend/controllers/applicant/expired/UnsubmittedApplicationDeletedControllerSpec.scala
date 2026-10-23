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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.expired

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class UnsubmittedApplicationDeletedControllerSpec
extends ControllerSpec:

  private val path: String = "/agent-registration/apply/expired/unsubmitted-application-deleted"

  object agentApplication:

    val expiredAsUnsubmitted: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterExpired

    val stillInProgress: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterGrsDataReceived

  "routes should have correct paths and methods" in:
    AppRoutes.apply.expired.UnsubmittedApplicationDeletedController.show shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path when we are authorised and have no application should render the expired application deleted page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplicationNoContent()
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Your application has been deleted - Apply for an agent services account - GOV.UK"
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()

  s"GET $path when an application still exists should redirect to landing endpoint" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.expiredAsUnsubmitted)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location") shouldBe Some(AppRoutes.apply.AgentApplicationController.landing.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()
