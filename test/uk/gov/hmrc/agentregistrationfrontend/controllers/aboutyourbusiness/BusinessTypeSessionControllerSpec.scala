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

package uk.gov.hmrc.agentregistrationfrontend.controllers.aboutyourbusiness

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentType.UkTaxAgent
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistrationfrontend.forms.BusinessTypeSessionForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class BusinessTypeSessionControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/about-your-business/business-type"
  private val stubAddAgentUrl = "/agent-registration/test-only/add-agent-type/uk-tax-agent"

  "routes should have correct paths and methods" in:
    routes.BusinessTypeSessionController.show shouldBe Call(
      method = "GET",
      url = path
    )
    routes.BusinessTypeSessionController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    routes.BusinessTypeSessionController.submit.url shouldBe routes.BusinessTypeSessionController.show.url

  s"GET $path without AgentType in session should return 303 and redirect to agent type page" in:
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.AgentTypeController.show.url

  s"GET $path with AgentType in session should return 200 and render page" in:
    val response: WSResponse = get(
      uri = path,
      cookies = addAgentTypeToSession(UkTaxAgent).extractCookies
    )

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "How is your business set up? - Apply for an agent services account - GOV.UK"

  s"POST $path selecting partnership should redirect to the type of partnership page" in:
    val response: WSResponse =
      post(
        uri = path,
        cookies = addAgentTypeToSession(UkTaxAgent).extractCookies
      )(Map(BusinessTypeSessionForm.key -> Seq("PartnershipType")))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.PartnershipTypeController.show.url

  s"POST $path without valid selection should return 400" in:
    val response: WSResponse =
      post(
        uri = path,
        cookies = addAgentTypeToSession(UkTaxAgent).extractCookies
      )(Map(BusinessTypeSessionForm.key -> Seq("")))

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.title() shouldBe "Error: How is your business set up? - Apply for an agent services account - GOV.UK"
