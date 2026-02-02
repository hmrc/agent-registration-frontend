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
import play.api.mvc.Results
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.action.MergeFormValue
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.retrieve.Credentials

import com.google.inject.Inject
import com.google.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class IndividualProvideDetailsWithApplicationRequest[A](
  val agentApplication: AgentApplication,
  override val individualProvidedDetails: IndividualProvidedDetailsToBeDeleted,
  override val internalUserId: InternalUserId,
  override val request: Request[A],
  override val credentials: Credentials
)
extends IndividualProvideDetailsRequest[A](
  individualProvidedDetails,
  internalUserId,
  request,
  credentials
):
  Errors.require(
    requirement = individualProvidedDetails.agentApplicationId === agentApplication._id,
    message =
      s"Sanity Check: ApplicationId from the request (${agentApplication._id.value}) must match the provided details " +
        s"retrieved from backend (${individualProvidedDetails.internalUserId.value}) (this should never happen)"
  )(using this)

object IndividualProvideDetailsWithApplicationRequest:

  given [B, T]: MergeFormValue[IndividualProvideDetailsWithApplicationRequest[B], T] =
    (
      r: IndividualProvideDetailsWithApplicationRequest[B],
      t: T
    ) =>
      new IndividualProvideDetailsWithApplicationRequest[B](
        agentApplication = r.agentApplication,
        individualProvidedDetails = r.individualProvidedDetails,
        internalUserId = r.internalUserId,
        request = r,
        credentials = r.credentials
      ) with FormValue[T]:
        override val formValue: T = t

@Singleton
class EnrichWithAgentApplicationAction @Inject() (
  agentApplicationService: AgentApplicationService
)(using ec: ExecutionContext)
extends ActionRefiner[IndividualProvideDetailsRequest, IndividualProvideDetailsWithApplicationRequest]
with RequestAwareLogging:

  override protected def executionContext: ExecutionContext = ec
  override protected def refine[A](request: IndividualProvideDetailsRequest[A]): Future[Either[Result, IndividualProvideDetailsWithApplicationRequest[A]]] =
    given r: IndividualProvideDetailsRequest[A] = request
    agentApplicationService
      .find(request.individualProvidedDetails.agentApplicationId)
      .map:
        case Some(agentApplication) =>
          Right(
            IndividualProvideDetailsWithApplicationRequest(
              agentApplication = agentApplication,
              individualProvidedDetails = request.individualProvidedDetails,
              internalUserId = request.internalUserId,
              request = request,
              credentials = request.credentials
            )
          )

        case None =>
          Left(
            Results.Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)
          )
