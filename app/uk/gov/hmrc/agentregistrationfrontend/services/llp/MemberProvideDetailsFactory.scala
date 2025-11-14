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
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetailsIdGenerator
import uk.gov.hmrc.agentregistration.shared.llp.ProvidedDetailsState.Started

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberProvideDetailsFactory @Inject() (
  clock: Clock,
  memberProvidedDetailsIdGenerator: MemberProvidedDetailsIdGenerator
):

  def makeNewMemberProvidedDetails(
    internalUserId: InternalUserId,
    agentApplicationId: AgentApplicationId
  ): MemberProvidedDetails = MemberProvidedDetails(
    _id = memberProvidedDetailsIdGenerator.nextMemberProvidedDetailsId(),
    internalUserId = internalUserId,
    agentApplicationId = agentApplicationId,
    createdAt = Instant.now(clock),
    providedDetailsState = Started
  )
