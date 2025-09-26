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

package uk.gov.hmrc.agentregistrationfrontend.controllers.aboutyourapplication

import com.softwaremill.quicklens.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessType.SoleTrader
import uk.gov.hmrc.agentregistration.shared.UserRole.Owner
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationFactory
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as appRoutes

class CheckYourAnswerControllerSpec
extends ControllerSpec:

  private val applicationFactory = app.injector.instanceOf[ApplicationFactory]
  private val path = s"/agent-registration/apply/about-your-application/check-your-answers"
  private val fakeAgentApplication: AgentApplication = applicationFactory
    .makeNewAgentApplication(tdAll.internalUserId)
    .modify(_.aboutYourApplication.businessType).setTo(Some(SoleTrader))
    .modify(_.aboutYourApplication.userRole).setTo(Some(Owner))

  "routes should have correct paths and methods" in:
    routes.CheckYourAnswerController.show shouldBe Call(
      method = "GET",
      url = "/agent-registration/apply/about-your-application/check-your-answers"
    )
    routes.CheckYourAnswerController.submit shouldBe Call(
      method = "POST",
      url = "/agent-registration/apply/about-your-application/check-your-answers"
    )
    routes.CheckYourAnswerController.submit.url shouldBe routes.CheckYourAnswerController.show.url

  s"GET $path should return 200 and render page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"

  s"POST $path with confirm and continue selection should redirect to start Grs journey Url" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication

    val response: WSResponse = post(path)(Map.empty)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe appRoutes.GrsController.startGrsJourney.url
