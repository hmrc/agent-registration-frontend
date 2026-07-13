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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class SaveForLaterControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private val path = "/agent-registration/conditions-not-yet-met/save-and-come-back-later"

  object agentApplication:

    val riskingCompletedFixable: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixable
    val afterResubmitted: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterResubmitted
    val nonFixableOutcome: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedNonFixable

  "route should have correct path and method" in:
    AppRoutes.fixablefailures.SaveForLaterController.show shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path when application outcome is failed fixable should render page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(
      application = agentApplication.riskingCompletedFixable
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Your progress will be saved until 27 August 2026 - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"GET $path when application outcome is not failed fixable should redirect to the application status page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(
      application = agentApplication.nonFixableOutcome
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location") shouldBe Some(
      AppRoutes.apply.AgentApplicationController.applicationStatus.url
    )
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"GET $path for already resubmitted should redirect to application status endpoint" in:
    ApplyStubHelper.stubsToSupplyBprToPage(
      application = agentApplication.afterResubmitted
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location") shouldBe Some(
      AppRoutes.apply.AgentApplicationController.applicationStatus.url
    )
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
