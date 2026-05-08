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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual.risking

import com.softwaremill.quicklens.modify
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.ProvideDetailsStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class RiskingProgressControllerSpec
extends ControllerSpec:

  private val linkId = tdAll.linkId

  val completeAgentApplication: AgentApplication = tdAll
    .agentApplicationLlpSections
    .sectionContactDetails
    .afterEmailAddressVerified
    .modify(_.applicationState)
    .setTo(ApplicationState.SentForRisking)

  object individualProvidedDetails:
    val finished: IndividualProvidedDetails = tdAll
      .providedDetails
      .afterFinished
      .copy(personReference = PersonReference("PREF0"))

  private val path = s"/agent-registration/provide-details/status/${linkId.value}"

  "RiskingProgressController should have the correct routes" in:
    AppRoutes.providedetails.riskingprogress.RiskingProgressController.show(linkId) shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path when risking progress is FailedNonFixable should return 200 and render failed non fixable page" in:
    ProvideDetailsStubHelper.stubRiskingProgress(
      agentApplication = completeAgentApplication,
      individualProvidedDetails = individualProvidedDetails.finished,
      riskingProgress = tdAll.applicationRiskingResponse.failedNonFixable
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Records show that you do not meet the registration conditions - Apply for an agent services account - GOV.UK"
    ProvideDetailsStubHelper.verifyRiskingProgressCalls(individualProvidedDetails.finished.personReference)
