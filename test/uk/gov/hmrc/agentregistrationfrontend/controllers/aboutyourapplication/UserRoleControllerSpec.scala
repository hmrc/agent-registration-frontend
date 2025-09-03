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

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.forms.UserRoleForm
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationFactory
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class UserRoleControllerSpec
extends ControllerSpec:

  private val applicationFactory = app.injector.instanceOf[ApplicationFactory]
  private val path = "/agent-registration/register/about-your-application/user-role"
  private val fakeAgentApplication: AgentApplication = applicationFactory.makeNewAgentApplication(tdAll.internalUserId)

  "routes should have correct paths and methods" in:
    routes.UserRoleController.show shouldBe Call(
      method = "GET",
      url = "/agent-registration/register/about-your-application/user-role"
    )
    routes.UserRoleController.submit shouldBe Call(
      method = "POST",
      url = "/agent-registration/register/about-your-application/user-role"
    )
    routes.UserRoleController.submit.url shouldBe routes.UserRoleController.show.url

  s"GET $path should return 200 and render page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Are you the owner of the business? - Apply for an agent services account - GOV.UK"

  s"POST $path with valid selection should redirect to the next page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication

    // TODO: it doesn't check that the view actually has the form with those fields
    val response: WSResponse = post(path)(Map(UserRoleForm.key -> Seq("Owner")))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "/agent-registration/register/about-your-application/check-your-answers"

  s"POST $path without valid selection should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)

    val response: WSResponse = post(path)(Map(UserRoleForm.key -> Seq("")))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Are you the owner of the business? - Apply for an agent services account - GOV.UK"
