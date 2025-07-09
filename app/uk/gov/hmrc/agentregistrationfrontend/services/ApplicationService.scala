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
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedUtrRequest
import uk.gov.hmrc.agentregistrationfrontend.model.application.Application
import uk.gov.hmrc.agentregistrationfrontend.model.application.ApplicationId
import uk.gov.hmrc.agentregistrationfrontend.model.application.SessionId
import uk.gov.hmrc.agentregistrationfrontend.repository.ApplicationRepo
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplicationService @Inject() (
  applicationRepo: ApplicationRepo,
  applicationFactory: ApplicationFactory
)(implicit ec: ExecutionContext)
extends RequestAwareLogging:

  def upsertNewApplication()(implicit request: AuthorisedUtrRequest[?]): Future[Application] =
    val application: Application = applicationFactory
      .makeNewApplication(sessionId = request.sessionId)

    upsert(application)
      .map { _ =>
        logger.info(s"Started new application [applicationId:${application.id.value}]")
        application
      }

  def get(applicationId: ApplicationId)(implicit request: Request[?]): Future[Application] = find(applicationId).map { maybeApplication =>
    maybeApplication
      .getOrElse(Errors.throwServerErrorException(s"Expected application to be found"))
  }

  def upsert[J <: Application](application: J)(implicit request: Request[?]): Future[J] =
    logger.info(s"Upserting new application...")
    applicationRepo
      .upsert(application)
      .map(_ => application)

  def find(applicationId: ApplicationId): Future[Option[Application]] = applicationRepo
    .findById(applicationId)

  def find(sessionId: SessionId): Future[Option[Application]] = applicationRepo
    .findBySessionId(sessionId)
