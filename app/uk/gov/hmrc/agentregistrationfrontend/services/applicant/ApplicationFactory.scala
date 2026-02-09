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

package uk.gov.hmrc.agentregistrationfrontend.services.applicant

import uk.gov.hmrc.agentregistration.shared.*

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationFactory @Inject() (
  clock: Clock,
  linkIdGenerator: LinkIdGenerator,
  agentApplicationIdGenerator: AgentApplicationIdGenerator
):

  def makeNewAgentApplicationSoleTrader(
    internalUserId: InternalUserId,
    groupId: GroupId,
    userRole: UserRole
  ): AgentApplicationSoleTrader = AgentApplicationSoleTrader(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    internalUserId = internalUserId,
    linkId = linkIdGenerator.nextLinkId(),
    groupId = groupId,
    createdAt = Instant.now(clock),
    applicationState = ApplicationState.Started,
    businessDetails = None,
    userRole = Some(userRole),
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    deceasedCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet
  )

  def makeNewAgentApplicationLlp(
    internalUserId: InternalUserId,
    groupId: GroupId,
    userRole: UserRole
  ): AgentApplicationLlp = AgentApplicationLlp(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    internalUserId = internalUserId,
    linkId = linkIdGenerator.nextLinkId(),
    groupId = groupId,
    createdAt = Instant.now(clock),
    applicationState = ApplicationState.Started,
    userRole = Some(userRole),
    businessDetails = None,
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    companyStatusCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    numberOfRequiredKeyIndividuals = None,
    numberOfOtherRelevantIndividuals = None
  )

  def makeNewAgentApplicationLimitedCompany(
    internalUserId: InternalUserId,
    groupId: GroupId,
    userRole: UserRole
  ): AgentApplicationLimitedCompany = AgentApplicationLimitedCompany(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    internalUserId = internalUserId,
    linkId = linkIdGenerator.nextLinkId(),
    groupId = groupId,
    createdAt = Instant.now(clock),
    applicationState = ApplicationState.Started,
    businessDetails = None,
    userRole = Some(userRole),
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    companyStatusCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    numberOfRequiredKeyIndividuals = None,
    numberOfOtherRelevantIndividuals = None
  )

  def makeNewAgentApplicationGeneralPartnership(
    internalUserId: InternalUserId,
    groupId: GroupId,
    userRole: UserRole
  ): AgentApplicationGeneralPartnership = AgentApplicationGeneralPartnership(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    internalUserId = internalUserId,
    linkId = linkIdGenerator.nextLinkId(),
    groupId = groupId,
    createdAt = Instant.now(clock),
    applicationState = ApplicationState.Started,
    businessDetails = None,
    userRole = Some(userRole),
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    numberOfRequiredKeyIndividuals = None,
    numberOfOtherRelevantIndividuals = None
  )

  def makeNewAgentApplicationLimitedPartnership(
    internalUserId: InternalUserId,
    groupId: GroupId,
    userRole: UserRole
  ): AgentApplicationLimitedPartnership = AgentApplicationLimitedPartnership(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    internalUserId = internalUserId,
    linkId = linkIdGenerator.nextLinkId(),
    groupId = groupId,
    createdAt = Instant.now(clock),
    applicationState = ApplicationState.Started,
    businessDetails = None,
    userRole = Some(userRole),
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    companyStatusCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    numberOfRequiredKeyIndividuals = None,
    numberOfOtherRelevantIndividuals = None
  )

  def makeNewAgentApplicationScottishLimitedPartnership(
    internalUserId: InternalUserId,
    groupId: GroupId,
    userRole: UserRole
  ): AgentApplicationScottishLimitedPartnership = AgentApplicationScottishLimitedPartnership(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    internalUserId = internalUserId,
    linkId = linkIdGenerator.nextLinkId(),
    groupId = groupId,
    createdAt = Instant.now(clock),
    applicationState = ApplicationState.Started,
    businessDetails = None,
    userRole = Some(userRole),
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    companyStatusCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    numberOfRequiredKeyIndividuals = None,
    numberOfOtherRelevantIndividuals = None
  )

  def makeNewAgentApplicationScottishPartnership(
    internalUserId: InternalUserId,
    groupId: GroupId,
    userRole: UserRole
  ): AgentApplicationScottishPartnership = AgentApplicationScottishPartnership(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    internalUserId = internalUserId,
    linkId = linkIdGenerator.nextLinkId(),
    groupId = groupId,
    createdAt = Instant.now(clock),
    applicationState = ApplicationState.Started,
    businessDetails = None,
    userRole = Some(userRole),
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    numberOfRequiredKeyIndividuals = None,
    numberOfOtherRelevantIndividuals = None
  )
