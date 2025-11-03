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

package uk.gov.hmrc.agentregistrationfrontend.action.providedetails

import play.api.mvc.Results.Redirect
import play.api.mvc.ActionRefiner
import play.api.mvc.Request
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.action.MergeFormValue
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.services.providedetails.ProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.retrieve.Credentials

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ProvideDetailsRequest[A](
  val providedDetails: ProvidedDetails,
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
    requirement = providedDetails.internalUserId === internalUserId,
    message =
      s"Sanity Check: InternalUserId from the request (${internalUserId.value}) must match the provided details " +
        s"retrieved from backend (${providedDetails.internalUserId.value}) (this should never happen)"
  )(using this)

object ProvideDetailsRequest:

  given [B, T]: MergeFormValue[ProvideDetailsRequest[B], T] =
    (
      r: ProvideDetailsRequest[B],
      t: T
    ) =>
      new ProvideDetailsRequest[B](
        providedDetails = r.providedDetails,
        internalUserId = r.internalUserId,
        request = r,
        credentials = r.credentials
      ) with FormValue[T]:
        override val formValue: T = t

@Singleton
class ProvideDetailsAction @Inject() (
  provideDetailsService: ProvideDetailsService
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  def apply(linkId: LinkId): ActionRefiner[IndividualAuthorisedRequest, ProvideDetailsRequest] =
    new ActionRefiner[IndividualAuthorisedRequest, ProvideDetailsRequest]:
      override protected def executionContext: ExecutionContext = ec

      override protected def refine[A](request: IndividualAuthorisedRequest[A]): Future[Either[Result, ProvideDetailsRequest[A]]] =
        given r: IndividualAuthorisedRequest[A] = request

        provideDetailsService
          .find(linkId)
          .flatMap {
            case Some(providedDetails) =>
              val redirectUrl: String = applicationRoutes.AgentApplicationController.genericExitPage.url
              logger.info(s"Request cannot be served by this microservice as details for member ${providedDetails.internalUserId} and application linkId ${providedDetails.linkId} are already provided")
              Future.successful(Left(Redirect(redirectUrl)))
            case None => createNewProvideDetails(linkId)
          }

  private def createNewProvideDetails[A](linkId: LinkId)(using request: IndividualAuthorisedRequest[A]): Future[Either[Result, ProvideDetailsRequest[A]]] =
    provideDetailsService
      .createNewProvidedDetails(linkId)
      .map(providedDetails =>
        Right(ProvideDetailsRequest[A](
          providedDetails = providedDetails,
          internalUserId = request.internalUserId,
          request = request.request,
          credentials = request.credentials
        ))
      )

  protected def executionContext: ExecutionContext = ec
