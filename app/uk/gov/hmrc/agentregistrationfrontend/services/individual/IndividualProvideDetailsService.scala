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

package uk.gov.hmrc.agentregistrationfrontend.services.individual

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistrationfrontend.connectors.IndividualProvidedDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class IndividualProvideDetailsService @Inject() (
  individualProvideDetailsConnector: IndividualProvidedDetailsConnector,
  provideDetailsFactory: IndividualProvideDetailsFactory
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  def create(
    individualName: IndividualName,
    isPersonOfControl: Boolean,
    agentApplicationId: AgentApplicationId
  )(using request: RequestHeader): IndividualProvidedDetails =
    logger.info(s"creating provided details for individual with applicationId:[${agentApplicationId.value}] ")
    provideDetailsFactory.create(
      agentApplicationId,
      individualName,
      isPersonOfControl
    )

  def upsert(individualProvidedDetails: IndividualProvidedDetails)(using request: RequestHeader): Future[Unit] =
    logger.debug(s"Upserting providedDetails for user:[${individualProvidedDetails._id}] and applicationId:[${individualProvidedDetails.agentApplicationId}]")
    individualProvideDetailsConnector
      .upsert(individualProvidedDetails)

  def findById(individualProvidedDetailsId: IndividualProvidedDetailsId)(using
    RequestHeader
  ): Future[Option[IndividualProvidedDetails]] = individualProvideDetailsConnector
    .findById(individualProvidedDetailsId)

  def delete(individualProvidedDetailsId: IndividualProvidedDetailsId)(using
    request: RequestHeader
  ): Future[Unit] =
    logger.debug(s"Deleting providedDetails for user:[${individualProvidedDetailsId.value}]")
    individualProvideDetailsConnector
      .delete(individualProvidedDetailsId)

  def createNewIndividualProvidedDetails(
    internalUserId: InternalUserId,
    agentApplicationId: AgentApplicationId,
    maybeIndividualNino: Option[IndividualNino],
    maybeIndividualSaUtr: Option[IndividualSaUtr],
    maybeIndividualDateOfBirth: Option[IndividualDateOfBirth] = None
  )(using request: RequestHeader): IndividualProvidedDetailsToBeDeleted =
    logger.info(s"creating new provided details for user:[${internalUserId.value}] and applicationId:[${agentApplicationId.value}] ")
    provideDetailsFactory.makeNewIndividualProvidedDetails(
      internalUserId,
      agentApplicationId,
      maybeIndividualNino,
      maybeIndividualSaUtr,
      maybeIndividualDateOfBirth
    )

  def findByApplicationId(applicationId: AgentApplicationId)(using request: RequestHeader): Future[Option[IndividualProvidedDetailsToBeDeleted]] =
    individualProvideDetailsConnector
      .find(applicationId)

  def findAll()(using request: RequestHeader): Future[List[IndividualProvidedDetailsToBeDeleted]] = individualProvideDetailsConnector
    .findAll()

  def upsert(individualProvidedDetails: IndividualProvidedDetailsToBeDeleted)(using request: RequestHeader): Future[Unit] =
    logger.debug(s"Upserting providedDetails for user:[${individualProvidedDetails.internalUserId}] and applicationId:[${individualProvidedDetails.agentApplicationId}]")
    individualProvideDetailsConnector
      .upsertMemberProvidedDetails(individualProvidedDetails)

  // for use by agent applicants when building lists of individuals
  def findAllByApplicationId(agentApplicationId: AgentApplicationId)(using request: RequestHeader): Future[List[IndividualProvidedDetails]] =
    individualProvideDetailsConnector.findAll(agentApplicationId)

  def markLinkSent(individualProvidedDetailsList: List[IndividualProvidedDetails])(using request: RequestHeader): Future[Unit] = {
    val upsertFutures: List[Future[Unit]] = individualProvidedDetailsList.map: individualProvidedDetails =>
      logger.debug(s"Marking link sent for providedDetails for user:[${individualProvidedDetails._id}] and applicationId:[${individualProvidedDetails.agentApplicationId}]")
      individualProvideDetailsConnector.upsert(
        individualProvidedDetails.copy(providedDetailsState = ProvidedDetailsState.AccessConfirmed)
      )

    Future.sequence(upsertFutures).map: _ =>
      ()
  }
