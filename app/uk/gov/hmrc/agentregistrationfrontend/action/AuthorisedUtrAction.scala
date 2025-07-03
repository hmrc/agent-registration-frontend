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
import play.api.mvc._
import uk.gov.hmrc.agentregistrationfrontend.model.Utr
import uk.gov.hmrc.agentregistrationfrontend.model.application.SessionId
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.views.ErrorResults

import scala.concurrent.{ExecutionContext, Future}

final class AuthorisedUtrRequest[A](
    val request: AuthenticatedRequest[A],
    val utr: Utr
)
  extends WrappedRequest[A](request) {
  val sessionId: SessionId = request.sessionId
}

@Singleton
class AuthorisedUtrAction @Inject()(
                                     errorResults: ErrorResults,
                                     cc:           MessagesControllerComponents
)
  extends ActionRefiner[AuthenticatedRequest, AuthorisedUtrRequest]
    with RequestAwareLogging {

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, AuthorisedUtrRequest[A]]] = {
    implicit val r: AuthenticatedRequest[A] = request
    val hasActiveSaEnrolment: Boolean = request.hasActiveSaEnrolment
    val maybeUtr: Option[Utr] = request.utr

    val result: Either[Result, AuthorisedUtrRequest[A]] =
      (hasActiveSaEnrolment, maybeUtr) match {
        case (_, None) =>
          logger.info("Authorisation outcome: Failed. Reason: - no present UTR")(request)
          Left(errorResults.unauthorised)
        case (false, _) =>
          logger.info("Authorisation outcome: Failed. Reason: - no active IR-SA enrolment")(request)
          Left(errorResults.unauthorised)
        case (true, Some(utr)) => Right(new AuthorisedUtrRequest[A](
            request = request,
            utr = utr
          ))
      }

    Future.successful(result)
  }
  implicit override protected def executionContext: ExecutionContext = cc.executionContext
}

