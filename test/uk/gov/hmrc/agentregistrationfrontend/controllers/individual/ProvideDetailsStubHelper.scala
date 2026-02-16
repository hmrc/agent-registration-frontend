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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll.tdAll
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs

object ProvideDetailsStubHelper:

  def stubAuthAndFindApplicationAndProvidedDetails(
    agentApplication: AgentApplication,
    individualProvideDetails: IndividualProvidedDetails,
    withBpr: Boolean = false
  ): StubMapping =
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvideDetails), agentApplication.agentApplicationId)
    AgentRegistrationStubs.stubFindApplicationByLinkId(tdAll.linkId, agentApplication)

  def stubAuthAndUpdateProvidedDetails(
    agentApplication: AgentApplication,
    individualProvidedDetails: IndividualProvidedDetails,
    updatedIndividualProvidedDetails: IndividualProvidedDetails
  ): StubMapping =
    stubAuthAndFindApplicationAndProvidedDetails(agentApplication, individualProvidedDetails)
    AgentRegistrationIndividualProvidedDetailsStubs.stubUpsertIndividualProvidedDetails(updatedIndividualProvidedDetails)

  def verifyAuthAndFindApplicationAndProvidedDetails(): Unit =
    AuthStubs.verifyAuthorise()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyFindAllForApplicationId(tdAll.agentApplicationId)
    AgentRegistrationStubs.verifyFindApplicationByLinkId(tdAll.linkId)

  def verifyAuthAndUpdateProvidedDetails(): Unit =
    verifyAuthAndFindApplicationAndProvidedDetails()
    AgentRegistrationIndividualProvidedDetailsStubs.verifyUpsertIndividualProvidedDetails()
