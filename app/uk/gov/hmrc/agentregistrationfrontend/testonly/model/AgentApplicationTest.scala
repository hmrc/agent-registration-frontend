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
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.SafeId

import java.time.Instant

extension (a: AgentApplication)
  /** `safeId` must match whatever `registeredBusinessPartnerId` the caller separately stores in agents-external-stubs' BPR record (see `FastForwardController`)
    * — the two are looked up together later when risking subscribes the agent, so a fresh random value must be threaded through both places consistently rather
    * than reusing the fixed value baked into the canned fixture.
    *
    * `providerId` must match the `userId` of the stub user actually signed in for this run (see `FastForwardController`) — agents-external-stubs' `/auth`
    * responses set `credId`/`credentials.gatewayId` to exactly `user.userId` (see `AuthStubController.Authority.prepareAuthorityResponse`), and risking later
    * uses `applicantCredentials.providerId` as the `userId` when allocating the new HMRC-AS-AGENT enrolment (`SubscriptionService.enrolAgent`) — a fixed,
    * unrelated providerId would allocate the enrolment to a user that was never actually signed in.
    */
  def withUpdatedIdentifiers(
    id: AgentApplicationId,
    internalUserId: InternalUserId,
    linkId: LinkId,
    groupId: GroupId,
    applicationReference: ApplicationReference,
    createdAt: Instant,
    safeId: SafeId,
    providerId: String
  ): AgentApplication =
    a match
      case a: AgentApplicationSoleTrader =>
        a.copy(
          _id = id,
          internalUserId = internalUserId,
          linkId = linkId,
          groupId = groupId,
          applicationReference = applicationReference,
          createdAt = createdAt,
          businessDetails = a.businessDetails.map(_.copy(safeId = safeId)),
          applicantCredentials = a.applicantCredentials.copy(providerId = providerId)
        )
      case a: AgentApplicationLlp =>
        a.copy(
          _id = id,
          internalUserId = internalUserId,
          linkId = linkId,
          groupId = groupId,
          applicationReference = applicationReference,
          createdAt = createdAt,
          businessDetails = a.businessDetails.map(_.copy(safeId = safeId)),
          applicantCredentials = a.applicantCredentials.copy(providerId = providerId)
        )
      case a: AgentApplicationLimitedCompany =>
        a.copy(
          _id = id,
          internalUserId = internalUserId,
          linkId = linkId,
          groupId = groupId,
          applicationReference = applicationReference,
          createdAt = createdAt,
          businessDetails = a.businessDetails.map(_.copy(safeId = safeId)),
          applicantCredentials = a.applicantCredentials.copy(providerId = providerId)
        )
      case a: AgentApplicationGeneralPartnership =>
        a.copy(
          _id = id,
          internalUserId = internalUserId,
          linkId = linkId,
          groupId = groupId,
          applicationReference = applicationReference,
          createdAt = createdAt,
          businessDetails = a.businessDetails.map(_.copy(safeId = safeId)),
          applicantCredentials = a.applicantCredentials.copy(providerId = providerId)
        )
      case a: AgentApplicationLimitedPartnership =>
        a.copy(
          _id = id,
          internalUserId = internalUserId,
          linkId = linkId,
          groupId = groupId,
          applicationReference = applicationReference,
          createdAt = createdAt,
          businessDetails = a.businessDetails.map(_.copy(safeId = safeId)),
          applicantCredentials = a.applicantCredentials.copy(providerId = providerId)
        )
      case a: AgentApplicationScottishLimitedPartnership =>
        a.copy(
          _id = id,
          internalUserId = internalUserId,
          linkId = linkId,
          groupId = groupId,
          applicationReference = applicationReference,
          createdAt = createdAt,
          businessDetails = a.businessDetails.map(_.copy(safeId = safeId)),
          applicantCredentials = a.applicantCredentials.copy(providerId = providerId)
        )
      case a: AgentApplicationScottishPartnership =>
        a.copy(
          _id = id,
          internalUserId = internalUserId,
          linkId = linkId,
          groupId = groupId,
          applicationReference = applicationReference,
          createdAt = createdAt,
          businessDetails = a.businessDetails.map(_.copy(safeId = safeId)),
          applicantCredentials = a.applicantCredentials.copy(providerId = providerId)
        )
