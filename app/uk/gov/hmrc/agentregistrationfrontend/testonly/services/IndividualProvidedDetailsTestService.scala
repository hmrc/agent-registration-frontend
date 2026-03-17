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

package uk.gov.hmrc.agentregistrationfrontend.testonly.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.connectors.IndividualProvidedDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class IndividualProvidedDetailsTestService @Inject() (
  individualProvideDetailsConnector: IndividualProvidedDetailsConnector,
  provideDetailsTestFactory: IndividualProvidedDetailsTestFactory
)
extends RequestAwareLogging:

  def create(
    individualName: IndividualName,
    isPersonOfControl: Boolean,
    agentApplicationId: AgentApplicationId,
    providedDetailsState: ProvidedDetailsState = ProvidedDetailsState.Precreated
  )(using request: RequestHeader): IndividualProvidedDetails =
    logger.info(s"creating provided details for individual with applicationId:[${agentApplicationId.value}] ")
    provideDetailsTestFactory.create(
      agentApplicationId,
      individualName,
      isPersonOfControl,
      providedDetailsState
    )

  def upsertForApplication(individualProvidedDetails: IndividualProvidedDetails)(using request: RequestHeader): Future[Unit] =
    logger.debug(s"Upserting providedDetails for user:[${individualProvidedDetails._id}] and applicationId:[${individualProvidedDetails.agentApplicationId}]")
    individualProvideDetailsConnector
      .upsert(individualProvidedDetails)
