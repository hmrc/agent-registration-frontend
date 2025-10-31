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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails

import com.softwaremill.quicklens.modify
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class StartControllerSpec
extends ControllerSpec:

  private val linkId = LinkId("test-link-id")

  private val path: String = s"/agent-registration/provide-details/start/${linkId.value}"

  object agentApplication:

    val inComplete: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAuthorised
        .afterEmailAddressVerified
    val complete: AgentApplicationLlp = inComplete
      .modify(_.applicationState)
      .setTo(ApplicationState.Submitted)

  "routes should have correct paths and methods" in:
    arf.providedetails.routes.StartController.start(linkId) shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should return 200 and render the start page" in:
    AgentRegistrationStubs.stubFindApplicationByLinkId(linkId = linkId, agentApplication = agentApplication.complete)
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Sign in and confirm your details - Apply for an agent services account - GOV.UK"

  s"GET $path with incomplete application should return 303 and redirect to an exit page" in:
    AgentRegistrationStubs.stubFindApplicationByLinkId(linkId = linkId, agentApplication = agentApplication.inComplete)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location") shouldBe Some("/agent-registration/apply/exit")
