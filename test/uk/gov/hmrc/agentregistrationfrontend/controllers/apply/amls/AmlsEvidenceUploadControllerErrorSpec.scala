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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.amls

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class AmlsEvidenceUploadControllerErrorSpec
extends ControllerSpec:

  private val uploadErrorPath = "/agent-registration/apply/anti-money-laundering/evidence/error"

  private object agentApplication:

    val afterAmlsExpiryDateProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterAmlsExpiryDateProvided

    val afterUploadFailed: AgentApplicationLlp = afterAmlsExpiryDateProvided

  "routes should have correct paths and methods" in:
    AppRoutes.apply.amls.AmlsEvidenceUploadController.showError(
      errorCode = Some("errorCode"),
      errorMessage = Some("errorMessage"),
      errorRequestId = Some("errorRequestId"),
      key = Some("key")
    ) shouldBe Call(
      method = "GET",
      url = uploadErrorPath + "?errorCode=errorCode&errorMessage=errorMessage&errorRequestId=errorRequestId&key=key"
    )

  s"GET $uploadErrorPath with params should render the error page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterAmlsExpiryDateProvided)
    val response: WSResponse = get(s"$uploadErrorPath?key=reference&errorRequestId=1&errorCode=EntityTooLarge&errorMessage=The%20file%20is%20too%20large")
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe "Your upload is too large - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
