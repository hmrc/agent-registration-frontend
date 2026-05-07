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
import uk.gov.hmrc.agentregistration.shared.audit.SessionId
import uk.gov.hmrc.auth.core.retrieve.Credentials

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
    cachedSessionId: SessionId,
    applicantCredentials: Credentials,
    groupId: GroupId,
    userRole: UserRole,
    applicationReference: ApplicationReference
  ): AgentApplicationSoleTrader = AgentApplicationSoleTrader(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    cachedSessionId = cachedSessionId,
    applicationReference = applicationReference,
    internalUserId = internalUserId,
    applicantCredentials = applicantCredentials,
    linkId = linkIdGenerator.nextLinkId(),
    groupId = groupId,
    createdAt = Instant.now(clock),
    submittedAt = None,
    applicationState = ApplicationState.Started,
    businessDetails = None,
    userRole = Some(userRole),
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    deceasedCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    hasOtherRelevantIndividuals = None,
    vrns = None,
    payeRefs = None
  )

  def makeNewAgentApplicationLlp(
    internalUserId: InternalUserId,
    cachedSessionId: SessionId,
    applicantCredentials: Credentials,
    groupId: GroupId,
    userRole: UserRole,
    applicationReference: ApplicationReference
  ): AgentApplicationLlp = AgentApplicationLlp(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    cachedSessionId = cachedSessionId,
    applicationReference = applicationReference,
    internalUserId = internalUserId,
    applicantCredentials = applicantCredentials,
    linkId = linkIdGenerator.nextLinkId(),
    groupId = groupId,
    createdAt = Instant.now(clock),
    submittedAt = None,
    applicationState = ApplicationState.Started,
    userRole = Some(userRole),
    businessDetails = None,
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    numberOfIndividuals = None,
    hasOtherRelevantIndividuals = None,
    vrns = None,
    payeRefs = None
  )

  def makeNewAgentApplicationLimitedCompany(
    internalUserId: InternalUserId,
    cachedSessionId: SessionId,
    applicantCredentials: Credentials,
    groupId: GroupId,
    userRole: UserRole,
    applicationReference: ApplicationReference
  ): AgentApplicationLimitedCompany = AgentApplicationLimitedCompany(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    cachedSessionId = cachedSessionId,
    applicationReference = applicationReference,
    internalUserId = internalUserId,
    applicantCredentials = applicantCredentials,
    linkId = linkIdGenerator.nextLinkId(),
    groupId = groupId,
    createdAt = Instant.now(clock),
    submittedAt = None,
    applicationState = ApplicationState.Started,
    businessDetails = None,
    userRole = Some(userRole),
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    numberOfIndividuals = None,
    hasOtherRelevantIndividuals = None,
    vrns = None,
    payeRefs = None
  )

  def makeNewAgentApplicationGeneralPartnership(
    internalUserId: InternalUserId,
    cachedSessionId: SessionId,
    applicantCredentials: Credentials,
    groupId: GroupId,
    userRole: UserRole,
    applicationReference: ApplicationReference
  ): AgentApplicationGeneralPartnership = AgentApplicationGeneralPartnership(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    cachedSessionId = cachedSessionId,
    applicationReference = applicationReference,
    internalUserId = internalUserId,
    applicantCredentials = applicantCredentials,
    linkId = linkIdGenerator.nextLinkId(),
    groupId = groupId,
    createdAt = Instant.now(clock),
    submittedAt = None,
    applicationState = ApplicationState.Started,
    businessDetails = None,
    userRole = Some(userRole),
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    numberOfIndividuals = None,
    hasOtherRelevantIndividuals = None,
    vrns = None,
    payeRefs = None
  )

  def makeNewAgentApplicationLimitedPartnership(
    internalUserId: InternalUserId,
    cachedSessionId: SessionId,
    applicantCredentials: Credentials,
    groupId: GroupId,
    userRole: UserRole,
    applicationReference: ApplicationReference
  ): AgentApplicationLimitedPartnership = AgentApplicationLimitedPartnership(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    cachedSessionId = cachedSessionId,
    applicationReference = applicationReference,
    internalUserId = internalUserId,
    applicantCredentials = applicantCredentials,
    linkId = linkIdGenerator.nextLinkId(),
    groupId = groupId,
    createdAt = Instant.now(clock),
    submittedAt = None,
    applicationState = ApplicationState.Started,
    businessDetails = None,
    userRole = Some(userRole),
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    numberOfIndividuals = None,
    hasOtherRelevantIndividuals = None,
    vrns = None,
    payeRefs = None
  )

  def makeNewAgentApplicationScottishLimitedPartnership(
    internalUserId: InternalUserId,
    cachedSessionId: SessionId,
    applicantCredentials: Credentials,
    groupId: GroupId,
    userRole: UserRole,
    applicationReference: ApplicationReference
  ): AgentApplicationScottishLimitedPartnership = AgentApplicationScottishLimitedPartnership(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    cachedSessionId = cachedSessionId,
    applicationReference = applicationReference,
    internalUserId = internalUserId,
    applicantCredentials = applicantCredentials,
    linkId = linkIdGenerator.nextLinkId(),
    groupId = groupId,
    createdAt = Instant.now(clock),
    submittedAt = None,
    applicationState = ApplicationState.Started,
    businessDetails = None,
    userRole = Some(userRole),
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    numberOfIndividuals = None,
    hasOtherRelevantIndividuals = None,
    vrns = None,
    payeRefs = None
  )

  def makeNewAgentApplicationScottishPartnership(
    internalUserId: InternalUserId,
    cachedSessionId: SessionId,
    applicantCredentials: Credentials,
    groupId: GroupId,
    userRole: UserRole,
    applicationReference: ApplicationReference
  ): AgentApplicationScottishPartnership = AgentApplicationScottishPartnership(
    _id = agentApplicationIdGenerator.nextApplicationId(),
    cachedSessionId = cachedSessionId,
    applicationReference = applicationReference,
    internalUserId = internalUserId,
    applicantCredentials = applicantCredentials,
    linkId = linkIdGenerator.nextLinkId(),
    groupId = groupId,
    createdAt = Instant.now(clock),
    submittedAt = None,
    applicationState = ApplicationState.Started,
    businessDetails = None,
    userRole = Some(userRole),
    applicantContactDetails = None,
    amlsDetails = None,
    agentDetails = None,
    refusalToDealWithCheckResult = None,
    hmrcStandardForAgentsAgreed = StateOfAgreement.NotSet,
    numberOfIndividuals = None,
    hasOtherRelevantIndividuals = None,
    vrns = None,
    payeRefs = None
  )
