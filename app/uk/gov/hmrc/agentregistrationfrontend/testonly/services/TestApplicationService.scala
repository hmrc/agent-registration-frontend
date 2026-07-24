/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.testonly.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistrationfrontend.connectors.AgentRegistrationConnector
import uk.gov.hmrc.agentregistrationfrontend.testonly.connectors.TestAgentRegistrationConnector
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.TestOnlyLink
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class TestApplicationService @Inject() (
  testAgentRegistrationConnector: TestAgentRegistrationConnector,
  agentRegistrationConnector: AgentRegistrationConnector
)
extends RequestAwareLogging:

  def makeTestApplication()(using request: RequestHeader): Future[TestOnlyLink] = testAgentRegistrationConnector
    .makeTestApplication()

  def deleteAll()(using RequestHeader): Future[Unit] = testAgentRegistrationConnector.deleteAllApplications()

  def findApplication(applicationReference: ApplicationReference)(using RequestHeader): Future[Option[AgentApplication]] =
    agentRegistrationConnector.findApplication(applicationReference)

  def findApplication(agentApplicationId: AgentApplicationId)(using RequestHeader): Future[Option[AgentApplication]] =
    testAgentRegistrationConnector.findApplication(agentApplicationId)

  def findIndividual(individualProvidedDetailsId: IndividualProvidedDetailsId)(using RequestHeader): Future[Option[IndividualProvidedDetails]] =
    testAgentRegistrationConnector.findIndividual(individualProvidedDetailsId)
