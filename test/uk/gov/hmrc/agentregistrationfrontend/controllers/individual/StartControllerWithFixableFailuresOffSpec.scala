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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import com.softwaremill.quicklens.modify
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll.tdAll.agentApplicationId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs

/** This test class is specifically for testing the StartController when the fixable failures feature is turned off, once the feature is turned on we can delete
  * this test class. It ensures that the controller behaves correctly in this configuration, particularly in terms of routing and response handling.
  */
class StartControllerWithFixableFailuresOffSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> false
    )

  private val linkId: LinkId = tdAll.linkId
  private val path: String = s"/agent-registration/provide-details/start/${linkId.value}"

  object agentApplication:

    val inComplete: AgentApplication =
      tdAll
        .agentApplicationLlpSections
        .sectionContactDetails
        .afterEmailAddressVerified
    val complete: AgentApplication = inComplete
      .modify(_.applicationState)
      .setTo(ApplicationState.SentForRisking)

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.StartController.start(linkId) shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should return 200 and render the start page" in:
    AgentRegistrationStubs.stubFindApplicationByLinkId(linkId = linkId, agentApplication = agentApplication.inComplete)
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindIndividualProvidedDetailsNoContent(agentApplicationId)
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Sign in and confirm your details - Apply for an agent services account - GOV.UK"
    AgentRegistrationStubs.verifyFindApplicationByLinkId(linkId = linkId)

  s"GET $path with complete application should return 303 and redirect to the status page" in:
    AgentRegistrationStubs.stubFindApplicationByLinkId(linkId = linkId, agentApplication = agentApplication.complete)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location") shouldBe Some("/agent-registration/provide-details/status/link-id-12345")
    AgentRegistrationStubs.verifyFindApplicationByLinkId(linkId = linkId)
