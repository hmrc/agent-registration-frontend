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

package uk.gov.hmrc.agentregistrationfrontend.services.llp

import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsIdGenerator
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Precreated
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Started

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndividualProvideDetailsFactory @Inject() (
  clock: Clock,
  individualProvidedDetailsIdGenerator: IndividualProvidedDetailsIdGenerator
):

  def makeNewIndividualProvidedDetails(
    internalUserId: InternalUserId,
    agentApplicationId: AgentApplicationId,
    individualNino: Option[IndividualNino],
    individualSaUtr: Option[IndividualSaUtr],
    individualDateOfBirth: Option[IndividualDateOfBirth]
  ): IndividualProvidedDetailsToBeDeleted = IndividualProvidedDetailsToBeDeleted(
    _id = individualProvidedDetailsIdGenerator.nextIndividualProvidedDetailsId(),
    internalUserId = internalUserId,
    agentApplicationId = agentApplicationId,
    createdAt = Instant.now(clock),
    providedDetailsState = Started,
    individualNino = individualNino,
    individualSaUtr = individualSaUtr,
    individualDateOfBirth = individualDateOfBirth
  )

  def create(
    agentApplicationId: AgentApplicationId,
    individualName: IndividualName,
    isPersonOfControl: Boolean
  ): IndividualProvidedDetails = IndividualProvidedDetails(
    _id = individualProvidedDetailsIdGenerator.nextIndividualProvidedDetailsId(),
    agentApplicationId = agentApplicationId,
    createdAt = Instant.now(clock),
    providedDetailsState = Precreated,
    individualName = individualName,
    isPersonOfControl = isPersonOfControl,
    internalUserId = None
  )
