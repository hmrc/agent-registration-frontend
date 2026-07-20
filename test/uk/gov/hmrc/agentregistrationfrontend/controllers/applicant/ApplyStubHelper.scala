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

  def stubsForSuccessfulUpdateWithBpr(
    application: AgentApplication,
    updatedApplication: AgentApplication
  ): StubMapping =
    stubsToSupplyBprToPage(application)
    AgentRegistrationStubs.stubUpdateAgentApplication(updatedApplication)

  def verifyConnectorsForSuccessfulUpdateWithBpr(): Unit =
    verifyConnectorsToSupplyBprToPage()
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
    AgentRegistrationStubs.stubFindIndividualsForApplication(application.agentApplicationId, List.empty)

  def verifyConnectorsForApplicationRiskingResponse(agentApplication: AgentApplication): Unit =
    verifyConnectorsToSupplyBprToPage(Some(agentApplication.getUtr))
    AgentRegistrationRiskingStubs.verifyGetApplicationRiskingResponse(agentApplication.applicationReference)

  def stubsForApplicationBprAndIndividuals(
    application: AgentApplication,
    individuals: List[IndividualProvidedDetails]
  ): StubMapping =
    stubsToSupplyBprToPage(application)
    AgentRegistrationStubs.stubFindIndividualsForApplication(application.agentApplicationId, individuals)

  def verifyConnectorsForApplicationBprAndIndividuals(agentApplication: AgentApplication): Unit =
    verifyConnectorsToSupplyBprToPage(Some(agentApplication.getUtr))
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.agentApplicationId)

  def stubsForUpdatingApplication(
    application: AgentApplication,
    updatedApplication: AgentApplication,
    individuals: List[IndividualProvidedDetails]
  ): StubMapping =
    stubsForApplicationBprAndIndividuals(application, individuals)
    AgentRegistrationStubs.stubUpdateAgentApplication(updatedApplication)

  def verifyConnectorsForUpdatingApplication(agentApplication: AgentApplication): Unit =
    verifyConnectorsForApplicationBprAndIndividuals(agentApplication)
    AgentRegistrationStubs.verifyUpdateAgentApplication()

  def stubsForApplicationBprAndIndividualsAndRisking(
    application: AgentApplication,
    individuals: List[IndividualProvidedDetails]
  ): StubMapping =
    stubsToSupplyBprToPage(application)
    AgentRegistrationStubs.stubFindIndividualsForApplication(application.agentApplicationId, individuals)
    // we don't need this when the fixable-failures feature is enabled, but it needs to be part of the request data until we can turn the flag on permanently
    // the type of risking progress doesn't matter as it's not consumed
    AgentRegistrationRiskingStubs.stubGetApplicationRiskingResponse(application.applicationReference, RiskingProgress.SubmittedForRisking)

  def stubFixableFailureUpdate(
    agentApplication: AgentApplication,
    individualProvidedDetails: IndividualProvidedDetails,
    updatedIndividualProvidedDetails: Option[IndividualProvidedDetails],
    updatedApplication: Option[AgentApplication]
  ): StubMapping =
    updatedApplication.fold(
      stubsForApplicationBprAndIndividuals(
        application = agentApplication,
        individuals = List(individualProvidedDetails)
      )
    )(updated =>
      stubsForUpdatingApplication(
        application = agentApplication,
        updatedApplication = updated,
        individuals = List(individualProvidedDetails)
      )
    )
    updatedIndividualProvidedDetails.map: updated =>
      AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(
        individualProvidedDetails = updated
      )
    AgentRegistrationStubs.stubGetApplicationBusinessPartnerRecord(
      utr = tdAll.saUtr.asUtr,
      responseBody = tdAll.businessPartnerRecordResponse
    )
