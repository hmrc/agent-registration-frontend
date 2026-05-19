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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.link

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class LinkStartPageControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/share-link-start"

  object agentApplication:

    val afterHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.link.LinkStartPageController.show shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should return 200 and render page explaining the link sharing process" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "We need information from the people you told us about - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
