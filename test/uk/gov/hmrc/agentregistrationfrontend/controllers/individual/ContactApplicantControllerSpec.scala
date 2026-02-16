/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
//import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
//import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs

class ContactApplicantControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/provide-details/record-not-found-exit-page"

  "route should have correct path and method" in:
    AppRoutes.providedetails.ContactApplicantController.show shouldBe Call(
      method = "GET",
      url = path
    )

//  AppRoutes.providedetails.IndividualHmrcStandardForAgentsController.submit.url shouldBe AppRoutes.providedetails.IndividualHmrcStandardForAgentsController.show.url

  s"GET $path should return 200 and render the start page" in:
//    AgentRegistrationStubs.stubFindApplicationByLinkId(linkId = linkId, agentApplication = agentApplication.inComplete)
//    AgentRegistrationIndividualProvidedDetailsStubs.stubFindIndividualProvidedDetailsNoContent(agentApplicationId)
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "You need to contact the applicant - Apply for an agent services account - GOV.UK"
    response.parseBodyAsJsoupDocument.body() shouldContainContent "You need to contact the applicant as we cannot match your details"
