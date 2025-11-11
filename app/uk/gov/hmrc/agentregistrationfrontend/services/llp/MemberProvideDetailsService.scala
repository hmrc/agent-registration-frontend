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
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.connectors.llp.MemberProvidedDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class MemberProvideDetailsService @Inject() (
  memberProvideDetailsConnector: MemberProvidedDetailsConnector,
  provideDetailsFactory: MemberProvideDetailsFactory
)
extends RequestAwareLogging:

  def createNewMemberProvidedDetails(applicationId: AgentApplicationId)(using request: IndividualAuthorisedRequest[?]): MemberProvidedDetails =
    logger.info(s"creating new provided details for user:[${request.internalUserId}] and applicationId:[${applicationId}] ")
    provideDetailsFactory.makeNewMemberProvidedDetails(request.internalUserId, applicationId)

  def findByApplicationId(applicationId: AgentApplicationId)(using request: IndividualAuthorisedRequest[?]): Future[Option[MemberProvidedDetails]] =
    memberProvideDetailsConnector
      .findMemberProvidedDetailsByApplicationId(applicationId)

  def upsert(memberProvidedDetails: MemberProvidedDetails)(using request: IndividualAuthorisedRequest[?]): Future[Unit] =
    logger.debug(s"Upserting providedDetails for user:[${memberProvidedDetails.internalUserId}] and applicationId:[${memberProvidedDetails.agentApplicationId}]")
    Errors.require(memberProvidedDetails.internalUserId === request.internalUserId, "Cannot modify provided details - you must be the user who created it")
    memberProvideDetailsConnector
      .upsertMemberProvidedDetails(memberProvidedDetails)
