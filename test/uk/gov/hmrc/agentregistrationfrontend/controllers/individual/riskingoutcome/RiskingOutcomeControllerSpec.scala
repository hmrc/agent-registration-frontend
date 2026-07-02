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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual.riskingoutcome

import com.softwaremill.quicklens.modify
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.ProvideDetailsStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class RiskingOutcomeControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private val linkId = tdAll.linkId

  val failedFixableApplication: AgentApplication = tdAll
    .agentApplicationLlpSections
    .sectionContactDetails
    .afterEmailAddressVerified
    .modify(_.applicationState)
    .setTo(ApplicationState.RiskingCompleted)
    .modify(_.riskingOutcomeApplication)
    .setTo(Some(RiskingOutcomeApplication(
      correctiveActionExpiryDate = Some(tdAll.correctiveActionExpiryDate),
      outcome = RiskingOutcomeApplication.Outcome.FailedFixable,
      riskingCompletedDate = tdAll.riskingCompletedDate
    )))

  val failedNonFixableApplication: AgentApplication = failedFixableApplication
    .modify(_.riskingOutcomeApplication)
    .setTo(Some(RiskingOutcomeApplication(
      correctiveActionExpiryDate = None,
      outcome = RiskingOutcomeApplication.Outcome.FailedNonFixable,
      riskingCompletedDate = tdAll.riskingCompletedDate
    )))

  object individualProvidedDetails:
    val finished: IndividualProvidedDetails = tdAll
      .providedDetails
      .afterFinished
      .copy(
        personReference = PersonReference("PREF0"),
        riskingOutcomeIndividual = Some(RiskingOutcomeIndividual.FailedFixable(
          fixes = Seq(IndividualFix._4._1(isConfirmed = None))
        ))
      )

  private val path = s"/agent-registration/provide-details/outcome-status/${linkId.value}"

  "RiskingOutcomeController should have the correct routes" in:
    AppRoutes.providedetails.riskingoutcome.RiskingOutcomeController.show(linkId) shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path when risking outcome is FailedNonFixable should return 200 and render failed non fixable page" in:
    ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
      agentApplication = failedNonFixableApplication,
      individualProvidedDetails = individualProvidedDetails.finished.copy(
        riskingOutcomeIndividual = Some(RiskingOutcomeIndividual.FailedNonFixable(
          failures = Seq(IndividualFailure._7)
        ))
      ),
      isScr = false
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Records show that you do not meet the registration conditions - Apply for an agent services account - GOV.UK"

  s"GET $path when risking outcome is FailedFixable should return 200 and render failed fixable page" in:
    ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
      agentApplication = failedFixableApplication,
      individualProvidedDetails = individualProvidedDetails.finished,
      isScr = false
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "You do not meet the registration conditions yet - Apply for an agent services account - GOV.UK"
