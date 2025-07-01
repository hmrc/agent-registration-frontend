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

import com.google.inject.{Inject, Singleton}
import play.api.mvc.Results._
import play.api.mvc._
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.model.Utr
import uk.gov.hmrc.agentregistrationfrontend.util.{Errors, RequestAwareLogging}
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.hc
import uk.gov.hmrc.agentregistrationfrontend.views.Views
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticatedAction @Inject() (
                                      af:           AuthorisedFunctions,
                                      views: Views,
                                      appConfig:   AppConfig,
                                      cc:           MessagesControllerComponents)(
    implicit
    ec: ExecutionContext
) extends ActionRefiner[Request, AuthenticatedRequest] with RequestAwareLogging {

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val r: Request[A] = request

    af.authorised().retrieve(
      Retrievals.allEnrolments and Retrievals.saUtr and Retrievals.credentials
    ).apply {
        case enrolments ~ utr ~ credentials =>
          val sessionId = hc.sessionId.getOrElse(Errors.throwServerErrorException("Expected Session ID to be present"))
          Future.successful(
            Right(new AuthenticatedRequest[A](
              request = request,
              enrolments = enrolments,
              utr = utr.map(Utr.apply),
              credentials = credentials,
              sessionId = sessionId
          )))
      }
      .recoverWith {
        case _: NoActiveSession =>
          //TODO: SafeRedirectUrl?
          Future.successful(Left(Redirect(
            url = appConfig.signInUri(uri"""${appConfig.thisFrontendBaseUrl}${request.uri}""").toString()
          )))
        case e: AuthorisationException =>
          logger.info(s"Authentication outcome: Failed. Unauthorised because of ${e.reason}, $e")
          views.unauthorised.map(Left(_))
      }
  }

  override protected def executionContext: ExecutionContext = cc.executionContext

}

