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

class UnsubmittedApplicationControllerSpec
extends ControllerSpec:

  private val path: String = "/agent-registration/apply/expired/unsubmitted-application"

  object agentApplication:

    val expiredAsUnsubmitted: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterExpired

    val stillInProgress: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterGrsDataReceived

    val afterRisking: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixable

  "routes should have correct paths and methods" in:
    AppRoutes.apply.expired.UnsubmittedApplicationController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.expired.UnsubmittedApplicationController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.expired.UnsubmittedApplicationController.submit.url shouldBe AppRoutes.apply.expired.UnsubmittedApplicationController.show.url

  s"GET $path when application has expired status and is unsubmitted should render the expired application page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.expiredAsUnsubmitted)
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Your application has expired - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path when application is still in progress should redirect to the landing endpoint" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.stillInProgress)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location") shouldBe Some(AppRoutes.apply.AgentApplicationController.landing.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path when application has been risked should redirect to the landing endpoint" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterRisking)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location") shouldBe Some(AppRoutes.apply.AgentApplicationController.landing.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path when application has expired status and is unsubmitted should delete the application and redirect to the expired application deleted page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.expiredAsUnsubmitted)
    ApplyStubHelper.stubsForDeleteAndStartAgain(agentApplication.expiredAsUnsubmitted)
    val response: WSResponse = post(path)(Map("deleteAndStartAgain" -> Seq("")))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location") shouldBe Some(AppRoutes.apply.expired.UnsubmittedApplicationDeletedController.show.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()
    ApplyStubHelper.verifyConnectorsForDeleteAndStartAgain()

  s"POST $path when risked application exists should not delete and redirect to the application landing page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterRisking)
    val response: WSResponse = post(path)(Map("deleteAndStartAgain" -> Seq("")))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location") shouldBe Some(AppRoutes.apply.AgentApplicationController.landing.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyDeleteAgentApplication(count = 0)
