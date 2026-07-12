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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.ApplicationData
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationRiskingStubs

class DeclarationControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private val path = "/agent-registration/conditions-not-yet-met/declaration"

  private val fixedIndividuals: List[IndividualProvidedDetails] = List(
    tdAll.providedDetails.afterFinished.copy(riskingOutcomeIndividual =
      Some(RiskingOutcomeIndividual.FailedFixable(
        fixes = Seq(
          IndividualFix._4._1(isConfirmed = Some(true))
        ),
        declarationAgreed = true
      ))
    )
  )

  val applicationData: ApplicationData =
    tdAll
      .agentApplicationLlp
      .applicationData

  object agentApplication:

    val riskingCompletedFixableFixed: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixableFixed
    val afterResubmitted: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterResubmitted

  "route should have correct path and method" in:
    AppRoutes.fixablefailures.DeclarationController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.fixablefailures.DeclarationController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.fixablefailures.DeclarationController.show.url shouldBe
      AppRoutes.fixablefailures.DeclarationController.submit.url

  s"GET $path for completed fixes including individual should render page" in:
    ApplyStubHelper.stubsForApplicationBprAndIndividuals(
      application = agentApplication.riskingCompletedFixableFixed,
      individuals = fixedIndividuals
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Declaration - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForApplicationBprAndIndividuals(agentApplication.riskingCompletedFixableFixed)

  s"GET $path for already resubmitted should redirect to application status endpoint" in:
    ApplyStubHelper.stubsForApplicationBprAndIndividuals(
      application = agentApplication.afterResubmitted,
      individuals = fixedIndividuals
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location") shouldBe Some(
      AppRoutes.apply.AgentApplicationController.applicationStatus.url
    )
    ApplyStubHelper.verifyConnectorsForApplicationBprAndIndividuals(agentApplication.afterResubmitted)

  s"POST $path for completed fixes including individual should redirect to application status endpoint" in:
    ApplyStubHelper.stubsForUpdatingApplication(
      application = agentApplication.riskingCompletedFixableFixed,
      updatedApplication = agentApplication.afterResubmitted,
      individuals = fixedIndividuals
    )
    AgentRegistrationRiskingStubs.stubSubmitAgentApplication(
      SubmitForRiskingRequest(
        applicationData = applicationData,
        individuals = List(tdAll.providedDetails.individualData),
        isResubmission = true
      )
    )
    val response: WSResponse = post(path)(Map("submit" -> Seq("AcceptAndSend")))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location") shouldBe Some(
      AppRoutes.apply.AgentApplicationController.applicationStatus.url
    )
    ApplyStubHelper.verifyConnectorsForUpdatingApplication(agentApplication.afterResubmitted)
    AgentRegistrationRiskingStubs.verifySubmitAgentApplication()
