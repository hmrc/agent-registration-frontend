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
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationMemberProvidedDetailsStubs

class MemberConfirmationControllerSpec
extends ControllerSpec:

  private val path: String = s"/agent-registration/provide-details/confirmation"

  object agentApplication:
    val applicationSubmitted: AgentApplicationLlp = tdAll
      .agentApplicationLlp
      .sectionAgentDetails
      .whenUsingExistingCompanyName
      .afterContactTelephoneSelected
      .modify(_.applicationState)
      .setTo(ApplicationState.Submitted)

  object memberProvidedDetails:

    val incompleteProvidedDetails: MemberProvidedDetails =
      tdAll
        .providedDetailsLlp
        .afterApproveAgentApplication

    val completedProvidedDetails: MemberProvidedDetails =
      tdAll
        .providedDetailsLlp
        .afterHmrcStandardforAgentsAgreed

  "routes should have correct paths and methods" in:
    routes.MemberConfirmationController.show shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should return 200 and render the confirmation page" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvidedDetails.completedProvidedDetails))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "You have finished this process - Apply for an agent services account - GOV.UK"

  s"GET $path with incomplete application should return 303 and redirect to check your answers page" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvidedDetails.incompleteProvidedDetails))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location") shouldBe Some(AppRoutes.providedetails.CheckYourAnswersController.show.url)
