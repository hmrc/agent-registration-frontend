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

package uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp

import play.api.mvc.ActionRefiner
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.action.MergeFormValue
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.services.llp.MemberProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class MemberProvideDetailsRequest[A](
  val memberProvidedDetails: MemberProvidedDetails,
  override val internalUserId: InternalUserId,
  override val request: Request[A],
  override val credentials: Credentials
)
extends IndividualAuthorisedRequest[A](
  internalUserId,
  request,
  credentials
):
  Errors.require(
    requirement = memberProvidedDetails.internalUserId === internalUserId,
    message =
      s"Sanity Check: InternalUserId from the request (${internalUserId.value}) must match the provided details " +
        s"retrieved from backend (${memberProvidedDetails.internalUserId.value}) (this should never happen)"
  )(using this)

object MemberProvideDetailsRequest:

  given [B, T]: MergeFormValue[MemberProvideDetailsRequest[B], T] =
    (
      r: MemberProvideDetailsRequest[B],
      t: T
    ) =>
      new MemberProvideDetailsRequest[B](
        memberProvidedDetails = r.memberProvidedDetails,
        internalUserId = r.internalUserId,
        request = r,
        credentials = r.credentials
      ) with FormValue[T]:
        override val formValue: T = t

@Singleton
class ProvideDetailsAction @Inject() (
  memberProvideDetailsService: MemberProvideDetailsService
)(using ec: ExecutionContext)
extends ActionRefiner[IndividualAuthorisedRequest, MemberProvideDetailsRequest]
with RequestAwareLogging:

  override protected def executionContext: ExecutionContext = ec

  override protected def refine[A](request: IndividualAuthorisedRequest[A]): Future[Either[Result, MemberProvideDetailsRequest[A]]] =
    given r: IndividualAuthorisedRequest[A] = request

    val redirect = AppRoutes.providedetails.SessionManagementController.sessionExpired

    Future(request.getAgentApplicationId)
      .flatMap: agentApplicationId =>
        memberProvideDetailsService
          .findByApplicationId(agentApplicationId)
          .flatMap:
            case Some(memberProvidedDetails) =>
              Future.successful(Right(new MemberProvideDetailsRequest(
                memberProvidedDetails = memberProvidedDetails,
                internalUserId = request.internalUserId,
                request = request.request,
                credentials = request.credentials
              )))
            case None =>
              logger.info(s"[Unexpected State] No member provided details found for authenticated user ${request.internalUserId.value}. Redirecting to ($redirect)")
              Future.successful(Left(Redirect(redirect)))
      .recoverWith:
        case e: IllegalStateException =>
          logger.info(
            s"[Missing data] AgentApplicationId not found in request for user ${request.internalUserId.value}. Redirecting to ($redirect)"
          )
          Future.successful(Left(Redirect(redirect)))
