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
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class FixableAmlsDetailsControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private val amlsFailureCodeHeadings: Map[String, String] = Map(
    "EntityFailure.3.1" -> "We could not match your details with a current record",
    "EntityFailure.3.2" -> "We have not been able to confirm your anti-money laundering supervision",
    "EntityFailure.3.3" -> "We could not accept your evidence",
    "EntityFailure.3.5" -> "Your membership does not include anti-money laundering supervision"
  )
  private val amlsFailures: Map[String, EntityFailure.IsAmls] = Map(
    "EntityFailure.3.1" -> (EntityFailure._3._1: EntityFailure.IsAmls),
    "EntityFailure.3.2" -> (EntityFailure._3._2: EntityFailure.IsAmls),
    "EntityFailure.3.3" -> (EntityFailure._3._3: EntityFailure.IsAmls),
    "EntityFailure.3.5" -> (EntityFailure._3._5: EntityFailure.IsAmls)
  )

  private def pathForFailureCode(failureCode: String) = s"/agent-registration/conditions-not-yet-met/anti-money-laundering/failure-details/$failureCode"
  object agentApplication:
    def riskingCompletedWithAmlsFix(failure: EntityFailure.IsAmls): AgentApplicationLlp = tdAll
      .agentApplicationLlp
      .afterRiskingCompletedWithFixableAmls(failure)

  amlsFailureCodeHeadings.foreach:
    (
      failureCode: String,
      amlsFailureHeading: String
    ) =>
      s"route for AMLS failure code $failureCode should have correct path and method" in:
        AppRoutes.fixablefailures.amlsfailure.FixableAmlsDetailsController.show(failureCode) shouldBe Call(
          method = "GET",
          url = pathForFailureCode(failureCode)
        )

      s"GET ${pathForFailureCode(failureCode)} should render correct content" in:
        val application = agentApplication.riskingCompletedWithAmlsFix(amlsFailures(failureCode))
        ApplyStubHelper.stubsToSupplyBprToPage(application = application)
        val response: WSResponse = get(pathForFailureCode(failureCode))

        response.status shouldBe Status.OK
        val doc = response.parseBodyAsJsoupDocument
        doc.title() shouldBe s"$amlsFailureHeading - Apply for an agent services account - GOV.UK"
        ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
