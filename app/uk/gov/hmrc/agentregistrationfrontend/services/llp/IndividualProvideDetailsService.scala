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
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.llp.IndividualNino
import uk.gov.hmrc.agentregistration.shared.llp.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*
import uk.gov.hmrc.agentregistrationfrontend.individual.action.IndividualAuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.individual.action.IndividualAuthorisedWithIdentifiersRequest
import uk.gov.hmrc.agentregistrationfrontend.connectors.IndividualProvidedDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class IndividualProvideDetailsService @Inject() (
  individualProvideDetailsConnector: IndividualProvidedDetailsConnector,
  provideDetailsFactory: IndividualProvideDetailsFactory
)
extends RequestAwareLogging:

  def createNewIndividualProvidedDetails(
    internalUserId: InternalUserId,
    agentApplicationId: AgentApplicationId,
    memberNino: Option[IndividualNino],
    memberSaUtr: Option[IndividualSaUtr],
    memberDateOfBirth: Option[LocalDate] = None
  )(using request: IndividualAuthorisedWithIdentifiersRequest[?]): IndividualProvidedDetails =
    logger.info(s"creating new provided details for user:[${internalUserId.value}] and applicationId:[${agentApplicationId.value}] ")
    provideDetailsFactory.makeNewIndividualProvidedDetails(
      internalUserId,
      agentApplicationId,
      memberNino,
      memberSaUtr,
      memberDateOfBirth
    )

  def findByApplicationId(applicationId: AgentApplicationId)(using request: IndividualAuthorisedRequest[?]): Future[Option[IndividualProvidedDetails]] =
    individualProvideDetailsConnector
      .find(applicationId)

  def findAll()(using request: IndividualAuthorisedRequest[?]): Future[List[IndividualProvidedDetails]] = individualProvideDetailsConnector
    .findAll()

  def upsert(individualProvidedDetails: IndividualProvidedDetails)(using request: IndividualAuthorisedRequest[?]): Future[Unit] =
    logger.debug(s"Upserting providedDetails for user:[${individualProvidedDetails.internalUserId}] and applicationId:[${individualProvidedDetails.agentApplicationId}]")
    Errors.require(individualProvidedDetails.internalUserId === request.internalUserId, "Cannot modify provided details - you must be the user who created it")
    individualProvideDetailsConnector
      .upsertMemberProvidedDetails(individualProvidedDetails)
