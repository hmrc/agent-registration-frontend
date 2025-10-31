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
import arf.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentTypeForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class AgentTypeControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/about-your-business/agent-type"

  "routes should have correct paths and methods" in:
    arf.aboutyourbusiness.routes.AgentTypeController.show shouldBe Call(
      method = "GET",
      url = path
    )
    arf.aboutyourbusiness.routes.AgentTypeController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    arf.aboutyourbusiness.routes.AgentTypeController.submit.url shouldBe arf.aboutyourbusiness.routes.AgentTypeController.show.url

  s"GET $path should return 200 and render page" in:
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Is your agent business based in the UK? - Apply for an agent services account - GOV.UK"

  s"POST $path with Yes should redirect to the next page" in:
    val response: WSResponse = post(path)(Map(AgentTypeForm.key -> Seq("UkTaxAgent")))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe arf.aboutyourbusiness.routes.BusinessTypeSessionController.show.url

  s"POST $path with No should redirect to an exit page" in:
    val response: WSResponse = post(path)(Map(AgentTypeForm.key -> Seq("NonUkTaxAgent")))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applicationRoutes.AgentApplicationController.genericExitPage.url

  s"POST $path without valid selection should return 400" in:
    val response: WSResponse = post(path)(Map(AgentTypeForm.key -> Seq("")))

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.title() shouldBe "Error: Is your agent business based in the UK? - Apply for an agent services account - GOV.UK"
