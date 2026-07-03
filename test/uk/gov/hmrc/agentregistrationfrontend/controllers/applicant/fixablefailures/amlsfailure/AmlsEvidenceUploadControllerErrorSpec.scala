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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures.amlsfailure

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class AmlsEvidenceUploadControllerErrorSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private val uploadErrorPath = "/agent-registration/conditions-not-yet-met/anti-money-laundering/evidence/upload-error"

  private object agentApplication:

    val riskingCompletedFixableAmls: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixableNonHmrcAmls

    val afterUploadFailed: AgentApplication = riskingCompletedFixableAmls

  "routes should have correct paths and methods" in:
    AppRoutes.fixablefailures.amlsfailure.AmlsEvidenceUploadController.showError(
      errorCode = Some("errorCode"),
      errorMessage = Some("errorMessage"),
      errorRequestId = Some("errorRequestId"),
      key = Some("key")
    ) shouldBe Call(
      method = "GET",
      url = uploadErrorPath + "?errorCode=errorCode&errorMessage=errorMessage&errorRequestId=errorRequestId&key=key"
    )

  s"GET $uploadErrorPath with params should render the error page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.riskingCompletedFixableAmls)
    val response: WSResponse = get(s"$uploadErrorPath?key=reference&errorRequestId=1&errorCode=EntityTooLarge&errorMessage=The%20file%20is%20too%20large")
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe "Your upload is too large - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
