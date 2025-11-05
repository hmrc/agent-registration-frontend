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
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.services.AgentRegistrationService
import uk.gov.hmrc.agentregistrationfrontend.services.providedetails.llp.MemberProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.retrieve.Credentials

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
  provideDetailsService: MemberProvideDetailsService,
  agentRegistrationService: AgentRegistrationService
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  def apply(linkId: LinkId): ActionRefiner[IndividualAuthorisedRequest, MemberProvideDetailsRequest] =
    new ActionRefiner[IndividualAuthorisedRequest, MemberProvideDetailsRequest]:
      override protected def executionContext: ExecutionContext = ec

      override protected def refine[A](request: IndividualAuthorisedRequest[A]): Future[Either[Result, MemberProvideDetailsRequest[A]]] =
        given r: IndividualAuthorisedRequest[A] = request

        agentRegistrationService.findApplicationByLinkId(linkId).flatMap {
          case Some(app) if app.hasFinished =>
            provideDetailsService
              .find()
              .flatMap {
                case Some(providedDetails) =>
                  logger.info(s"Request cannot be served by this microservice as details for member ${providedDetails.internalUserId} and application id ${providedDetails.applicationId} are already provided")
                  Future.successful(Left(Redirect(applicationRoutes.AgentApplicationController.genericExitPage.url)))
                case None => createNewProvideDetails(app.agentApplicationId)
              }
          case _ => Future.successful(Left(Redirect(applicationRoutes.AgentApplicationController.genericExitPage.url)))
        }

  private def createNewProvideDetails[A](agentApplicationId: AgentApplicationId)(using
    request: IndividualAuthorisedRequest[A]
  ): Future[Either[Result, MemberProvideDetailsRequest[A]]] = provideDetailsService
    .createNewMemberProvidedDetails(agentApplicationId)
    .map(memberProvidedDetails =>
      Right(MemberProvideDetailsRequest[A](
        memberProvidedDetails = memberProvidedDetails,
        internalUserId = request.internalUserId,
        request = request.request,
        credentials = request.credentials
      ))
    )

  protected def executionContext: ExecutionContext = ec
