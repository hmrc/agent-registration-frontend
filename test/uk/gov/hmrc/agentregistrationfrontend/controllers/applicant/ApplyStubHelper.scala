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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll.tdAll
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationRiskingStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

object ApplyStubHelper:

  def stubsForAuthAction(application: AgentApplication): StubMapping =
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(application)

  def verifyConnectorsForAuthAction(): Unit =
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()

  def stubsForSuccessfulUpdate(
    application: AgentApplication,
    updatedApplication: AgentApplication
  ): StubMapping =
    stubsForAuthAction(application)
    AgentRegistrationStubs.stubUpdateAgentApplication(updatedApplication)

  def verifyConnectorsForSuccessfulUpdate(): Unit =
    verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyUpdateAgentApplication()

  def stubsForDeleteAndStartAgain(application: AgentApplication): StubMapping =
    stubsForAuthAction(application)
    AgentRegistrationStubs.stubDeleteAgentApplication

  def verifyConnectorsForDeleteAndStartAgain(): Unit =
    verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyDeleteAgentApplication()

  def stubsToSupplyBprToPage(
    application: AgentApplication
  ): StubMapping =
    stubsForAuthAction(application)
    application match
      case a: AgentApplication.IsSoleTrader =>
        AgentRegistrationStubs.stubGetBusinessPartnerRecord(
          utr = a.getUtr,
          responseBody = tdAll.businessPartnerRecordResponseSoleTrader
        )
      case a: AgentApplication.IsNotSoleTrader =>
        AgentRegistrationStubs.stubGetBusinessPartnerRecord(
          utr = a.getUtr,
          responseBody = tdAll.businessPartnerRecordResponse
        )

  def verifyConnectorsToSupplyBprToPage(utr: Option[Utr] = None): Unit =
    verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(utr.getOrElse(tdAll.saUtr.asUtr))

  def stubsForApplicationRiskingResponse(
    application: AgentApplication,
    riskingProgress: RiskingProgress
  ): StubMapping =
    stubsToSupplyBprToPage(application)
    AgentRegistrationRiskingStubs.stubGetApplicationRiskingResponse(application.applicationReference, riskingProgress)

  def verifyConnectorsForApplicationRiskingResponse(agentApplication: AgentApplication): Unit =
    verifyConnectorsToSupplyBprToPage(Some(agentApplication.getUtr))
    AgentRegistrationRiskingStubs.verifyGetApplicationRiskingResponse(agentApplication.applicationReference)

  def stubsForTaskListPage(
    application: AgentApplication,
    individuals: List[IndividualProvidedDetails]
  ): StubMapping =
    stubsToSupplyBprToPage(application)
    AgentRegistrationStubs.stubFindIndividualsForApplication(application.agentApplicationId, individuals)

  def verifyConnectorsForTaskListPage(agentApplication: AgentApplication): Unit =
    verifyConnectorsToSupplyBprToPage(Some(agentApplication.getUtr))
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.agentApplicationId)
