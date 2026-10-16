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

class DeleteApplicationControllerSpec
extends ControllerSpec:

  private val path: String = "/agent-registration/apply/delete-application"

  object agentApplication:

    val beforeRisking: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterGrsDataReceived

    val duringRisking: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterSentForRisking

    val afterRisking: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixable

  "routes should have correct paths and methods" in:
    AppRoutes.apply.DeleteApplicationController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.DeleteApplicationController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.DeleteApplicationController.submit.url shouldBe AppRoutes.apply.DeleteApplicationController.show.url

  s"GET $path when risked application exists should render the confirm delete and start again page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterRisking)
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Delete your application - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path when application has not yet been risked should redirect to the landing endpoint" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeRisking)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location") shouldBe Some(AppRoutes.apply.AgentApplicationController.landing.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path when application is in the process of being risked should redirect to the landing endpoint" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.duringRisking)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location") shouldBe Some(AppRoutes.apply.AgentApplicationController.landing.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path when risked application exists and user confirms deletion should delete the application and redirect to the application deleted page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterRisking)
    ApplyStubHelper.stubsForDeleteAndStartAgain(agentApplication.afterRisking)
    val response: WSResponse = post(path)(Map("confirmDeleteApplication" -> Seq("Yes")))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location") shouldBe Some(AppRoutes.apply.AgentApplicationController.applicationDeleted.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()
    ApplyStubHelper.verifyConnectorsForDeleteAndStartAgain()

  s"POST $path when risked application exists and user does not confirm deletion should not delete and redirect to the application status page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterRisking)
    val response: WSResponse = post(path)(Map("confirmDeleteApplication" -> Seq("No")))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location") shouldBe Some(AppRoutes.apply.AgentApplicationController.applicationStatus.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyDeleteAgentApplication(count = 0)

  s"POST $path when risked application exists and user submits an invalid form should not delete and return a bad request with the form errors" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterRisking)
    val response: WSResponse = post(path)(Map("confirmDeleteApplication" -> Seq("")))
    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.title() shouldBe "Error: Delete your application - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyDeleteAgentApplication(count = 0)

  s"POST $path when application has not yet been risked should not delete and redirect to the landing endpoint" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeRisking)
    val response: WSResponse = post(path)(Map("confirmDeleteApplication" -> Seq("Yes")))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location") shouldBe Some(AppRoutes.apply.AgentApplicationController.landing.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyDeleteAgentApplication(count = 0)

  s"POST $path when application is in the process of being risked should not delete and redirect to the landing endpoint" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.duringRisking)
    val response: WSResponse = post(path)(Map("confirmDeleteApplication" -> Seq("Yes")))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location") shouldBe Some(AppRoutes.apply.AgentApplicationController.landing.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyDeleteAgentApplication(count = 0)
