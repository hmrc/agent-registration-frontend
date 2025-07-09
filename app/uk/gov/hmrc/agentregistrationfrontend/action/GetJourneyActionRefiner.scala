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

package uk.gov.hmrc.agentregistrationfrontend.action

import play.api.mvc.Results.Redirect
import play.api.mvc.ActionRefiner
import play.api.mvc.Request
import play.api.mvc.Result
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class GetApplicationActionRefiner @Inject() (
  applicationService: ApplicationService,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
extends ActionRefiner[AuthorisedUtrRequest, ApplicationRequest]
with RequestAwareLogging:

  override protected def refine[A](request: AuthorisedUtrRequest[A]): Future[Either[Result, ApplicationRequest[A]]] =
    implicit val r: Request[A] = request

    for
      maybeApplication <- applicationService.find(request.sessionId)
    yield maybeApplication match
      case Some(application) => Right(new ApplicationRequest(application, request))
      case None =>
        val redirect = uk.gov.hmrc.agentregistrationfrontend.controllers.routes.ApplicationController.initializeApplication
        logger.warn(s"Application not found based on the sessionId from session, redirecting to ${redirect.url}")
        Left(Redirect(redirect))

  override protected def executionContext: ExecutionContext = ec
