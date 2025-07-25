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

package uk.gov.hmrc.agentregistrationfrontend.services

import play.api.mvc.Request
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.connectors.AgentRegistrationConnector
import uk.gov.hmrc.agentregistrationfrontend.model.application.AgentRegistrationApplication
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.SafeEquals.===

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplicationService @Inject() (
  agentRegistrationConnector: AgentRegistrationConnector,
  applicationFactory: ApplicationFactory
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  def upsertNewApplication()(using request: AuthorisedRequest[?]): Future[AgentRegistrationApplication] =
    val application: AgentRegistrationApplication = applicationFactory.makeNewApplication(request.internalUserId)
    logger.info(s"Upserting new application [${request.internalUserId}]")
    upsert(application).map(_ => application)

  def find()(using request: AuthorisedRequest[?]): Future[Option[AgentRegistrationApplication]] = agentRegistrationConnector
    .findApplication()

  def get()(using request: AuthorisedRequest[?]): Future[AgentRegistrationApplication] = find()
    .map { maybeApplication =>
      maybeApplication.getOrElse(Errors.throwServerErrorException(s"Expected application to be found"))
    }

  def upsert(application: AgentRegistrationApplication)(using request: AuthorisedRequest[?]): Future[Unit] =
    Errors.require(application.internalUserId === request.internalUserId, "Cannot modify application - you must be the user who created it")
    agentRegistrationConnector
      .upsertApplication(application)
