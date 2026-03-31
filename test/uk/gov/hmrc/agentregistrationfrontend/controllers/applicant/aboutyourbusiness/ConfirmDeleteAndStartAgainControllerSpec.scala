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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.aboutyourbusiness

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class ConfirmDeleteAndStartAgainControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/about-your-business/start-again"

  "route should have correct paths and methods" in:
    AppRoutes.apply.aboutyourbusiness.ConfirmDeleteAndStartAgainController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.aboutyourbusiness.ConfirmDeleteAndStartAgainController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.aboutyourbusiness.ConfirmDeleteAndStartAgainController.submit.url shouldBe AppRoutes.apply.aboutyourbusiness.ConfirmDeleteAndStartAgainController.show.url

  s"GET $path should return 200 and offer start again button" in:
    ApplyStubHelper.stubsForAuthAction(tdAll.agentApplicationLlp.afterGrsDataReceived)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Confirm you want to delete your application and start again - Apply for an agent services account - GOV.UK"
    doc.h2Caption shouldBe "About your business"
    doc.select(".govuk-button[type=submit]").text() shouldBe "Start again"
    doc.select(".govuk-button.govuk-button--secondary").text() shouldBe "Cancel"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path should delete application and redirect to start of registration journey" in:
    ApplyStubHelper.stubsForDeleteAndStartAgain(tdAll.agentApplicationLlp.afterGrsDataReceived)
    val response: WSResponse =
      post(path)(
        Map("submit" -> Seq("DeleteAndStartAgain"))
      )
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.AgentApplicationController.startRegistration.url
