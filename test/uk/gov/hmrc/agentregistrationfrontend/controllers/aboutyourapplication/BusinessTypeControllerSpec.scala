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
import uk.gov.hmrc.agentregistrationfrontend.forms.BusinessTypeForm
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationFactory
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class BusinessTypeControllerSpec
extends ControllerSpec:

  private val applicationFactory = app.injector.instanceOf[ApplicationFactory]
  private val path = "/agent-registration/apply/about-your-application/business-type"
  private val fakeAgentApplication: AgentApplication = applicationFactory.makeNewAgentApplication(tdAll.internalUserId)

  "routes should have correct paths and methods" in:
    routes.BusinessTypeController.show shouldBe Call(
      method = "GET",
      url = "/agent-registration/apply/about-your-application/business-type"
    )
    routes.BusinessTypeController.submit shouldBe Call(
      method = "POST",
      url = "/agent-registration/apply/about-your-application/business-type"
    )
    routes.BusinessTypeController.submit.url shouldBe routes.BusinessTypeController.show.url

  s"GET $path should return 200 and render page" in:

    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "How is your business set up? - Apply for an agent services account - GOV.UK"

  s"POST $path with valid selection should redirect to the next page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    AgentRegistrationStubs.stubUpdateAgentApplication

    val response: WSResponse = post(path)(Map(BusinessTypeForm.key -> Seq("SoleTrader")))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "/agent-registration/apply/about-your-application/user-role"

  s"POST $path without valid selection should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)

    // TODO: finish exploring how to obtain submission form data out of form
//    val data: Map[String, Seq[String]] = BusinessTypeForm.form.mapping.mappings.map(_.key).map(k => k -> Seq("")).toMap
    val response: WSResponse = post(path)(Map(BusinessTypeForm.key -> Seq("")))

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.title() shouldBe "Error: How is your business set up? - Apply for an agent services account - GOV.UK"
