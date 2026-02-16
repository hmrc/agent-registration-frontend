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

package uk.gov.hmrc.agentregistrationfrontend.testonly.model

import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId

import java.time.Instant

extension (a: AgentApplication)
  def withUpdatedIdentifiers(
    id: AgentApplicationId,
    internalUserId: InternalUserId,
    linkId: LinkId,
    groupId: GroupId,
    createdAt: Instant
  ): AgentApplication =
    a match
      case a: AgentApplicationSoleTrader =>
        a.copy(
          _id = id,
          internalUserId = internalUserId,
          linkId = linkId,
          groupId = groupId,
          createdAt = createdAt
        )
      case a: AgentApplicationLlp =>
        a.copy(
          _id = id,
          internalUserId = internalUserId,
          linkId = linkId,
          groupId = groupId,
          createdAt = createdAt
        )
      case a: AgentApplicationLimitedCompany =>
        a.copy(
          _id = id,
          internalUserId = internalUserId,
          linkId = linkId,
          groupId = groupId,
          createdAt = createdAt
        )
      case a: AgentApplicationGeneralPartnership =>
        a.copy(
          _id = id,
          internalUserId = internalUserId,
          linkId = linkId,
          groupId = groupId,
          createdAt = createdAt
        )
      case a: AgentApplicationLimitedPartnership =>
        a.copy(
          _id = id,
          internalUserId = internalUserId,
          linkId = linkId,
          groupId = groupId,
          createdAt = createdAt
        )
      case a: AgentApplicationScottishLimitedPartnership =>
        a.copy(
          _id = id,
          internalUserId = internalUserId,
          linkId = linkId,
          groupId = groupId,
          createdAt = createdAt
        )
      case a: AgentApplicationScottishPartnership =>
        a.copy(
          _id = id,
          internalUserId = internalUserId,
          linkId = linkId,
          groupId = groupId,
          createdAt = createdAt
        )
