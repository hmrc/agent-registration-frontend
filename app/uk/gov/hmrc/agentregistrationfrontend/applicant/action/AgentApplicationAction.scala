/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.applicant.action

import play.api.mvc.Results.Redirect
import play.api.mvc.ActionRefiner
import play.api.mvc.Request
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistrationfrontend.shared.action.MergeFormValue
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*
import uk.gov.hmrc.agentregistrationfrontend.shared.action.FormValue

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AgentApplicationAction @Inject() (
  agentApplicationService: AgentApplicationService
)(using ec: ExecutionContext)
extends ActionRefiner[AuthorisedRequest, AgentApplicationRequest]
with RequestAwareLogging:

  override protected def refine[A](request: AuthorisedRequest[A]): Future[Either[Result, AgentApplicationRequest[A]]] =
    given r: AuthorisedRequest[A] = request
    agentApplicationService
      .find()
      .flatMap:
        case Some(application) =>
          Future.successful(Right(new AgentApplicationRequest(
            agentApplication = application,
            internalUserId = request.internalUserId,
            groupId = request.groupId,
            request = request.request,
            credentials = request.credentials
          )))
        case None =>
          val redirect = AppRoutes.apply.AgentApplicationController.startRegistration
          logger.error(s"[Unexpected State] No agent application found for authenticated user ${request.internalUserId.value}. Redirecting to startRegistration page ($redirect)")
          Future.successful(Left(Redirect(redirect)))

  override protected def executionContext: ExecutionContext = ec

class AgentApplicationRequest[A](
  val agentApplication: AgentApplication,
  override val internalUserId: InternalUserId,
  override val groupId: GroupId,
  override val request: Request[A],
  override val credentials: Credentials
)
extends AuthorisedRequest[A](
  internalUserId,
  groupId,
  request,
  credentials
):
  Errors.require(
    requirement = agentApplication.internalUserId === internalUserId,
    message =
      s"Sanity Check: InternalUserId from the request (${internalUserId.value}) must match the Application " +
        s"retrieved from backend (${agentApplication.internalUserId.value}) (this should never happen)"
  )(using this)

object AgentApplicationRequest:

  given [B, T]: MergeFormValue[AgentApplicationRequest[B], T] =
    (
      r: AgentApplicationRequest[B],
      t: T
    ) =>
      new AgentApplicationRequest[B](
        agentApplication = r.agentApplication,
        internalUserId = r.internalUserId,
        groupId = r.groupId,
        request = r,
        credentials = r.credentials
      ) with FormValue[T]:
        override val formValue: T = t
