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

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.llp.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.llp.IndividualNino
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistration.shared.llp.IndividualSaUtr
import uk.gov.hmrc.agentregistrationfrontend.connectors.IndividualProvidedDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class IndividualProvideDetailsService @Inject() (
  individualProvideDetailsConnector: IndividualProvidedDetailsConnector,
  provideDetailsFactory: IndividualProvideDetailsFactory
)
extends RequestAwareLogging:

  def preCreateIndividualProvidedDetails(
    individualName: IndividualName,
    isPersonOfControl: Boolean,
    agentApplicationId: AgentApplicationId
  )(using request: RequestHeader): IndividualProvidedDetails =
    logger.info(s"pre-creating provided details for nominated person of control ${individualName.value} and applicationId:[${agentApplicationId.value}] ")
    provideDetailsFactory.preCreateIndividualProvidedDetails(
      agentApplicationId,
      individualName,
      isPersonOfControl
    )

  def upsertPreCreatedProvidedDetails(individualProvidedDetails: IndividualProvidedDetails)(using request: RequestHeader): Future[Unit] =
    logger.debug(s"Upserting precreated providedDetails for user:[${individualProvidedDetails.individualName.value}] and applicationId:[${individualProvidedDetails.agentApplicationId}]")
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
