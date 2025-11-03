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

package uk.gov.hmrc.agentregistrationfrontend.services.providedetails

import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.connectors.providedetails.ProvideDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.util.{Errors, RequestAwareLogging}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProvideDetailsService @Inject()(
  provideDetailsConnector: ProvideDetailsConnector,
  provideDetailsFactory: ProvideDetailsFactory
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  def createNewProvidedDetails(linkId: LinkId)(using request: IndividualAuthorisedRequest[?]): Future[ProvidedDetails] =
    logger.info(s"creating new provided details for user:[${request.internalUserId}] and linkId:[${linkId}] ")
    Future.successful(provideDetailsFactory.makeNewProvidedDetails(request.internalUserId, linkId))


  def upsertNewProvidedDetails(linkId: LinkId)(using request: IndividualAuthorisedRequest[?]): Future[ProvidedDetails] =
    val providedDetails: ProvidedDetails = provideDetailsFactory.makeNewProvidedDetails(request.internalUserId, linkId)
    logger.info(s"Upserting new provided details for user:[${request.internalUserId}] and linkId:[${linkId}] ")
    upsert(providedDetails).map(_ => providedDetails)

  def find(linkId: LinkId)(using request: IndividualAuthorisedRequest[?]): Future[Option[ProvidedDetails]] = provideDetailsConnector
    .findProvidedDetails(linkId)

  def upsert(providedDetails: ProvidedDetails)(using request: IndividualAuthorisedRequest[?]): Future[Unit] =
    logger.debug(s"Upserting providedDetails for user:[${providedDetails.internalUserId}] and linkId:[${providedDetails.linkId}]")
    Errors.require(providedDetails.internalUserId === request.internalUserId, "Cannot modify provided details - you must be the user who created it")
    provideDetailsConnector
      .upsertProvidedDetails(providedDetails)
