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

import play.api.mvc.{ActionRefiner, Request, Result}
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.agentregistrationfrontend.model.application.AgentRegistrationApplication
import uk.gov.hmrc.agentregistrationfrontend.model.{GroupId, InternalUserId}
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.util.{Errors, RequestAwareLogging}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

class AgentRegistrationApplicationRequest[A](
  val agentRegistrationApplication: AgentRegistrationApplication,
  override val internalUserId: InternalUserId,
  override val groupId: GroupId,
  override val request: Request[A]
)
extends AuthorisedRequest[A](internalUserId, groupId, request):
  Errors.require(
    requirement = agentRegistrationApplication.internalUserId === internalUserId,
    message = s"Sanity Check: InternalUserId from the request (${internalUserId.value}) must match the Application " +
      s"retrieved from backend (${agentRegistrationApplication.internalUserId.value}) (this should never happen)"
  )(using this)

@Singleton
class AgentRegistrationApplicationAction @Inject()(
  applicationService: ApplicationService,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends ActionRefiner[AuthorisedRequest, AgentRegistrationApplicationRequest]
with RequestAwareLogging:

  override protected def refine[A](request: AuthorisedRequest[A]): Future[Either[Result, AgentRegistrationApplicationRequest[A]]] =
    given r: AuthorisedRequest[A] = request

    applicationService
      .find()
      .flatMap{ maybeApplication => maybeApplication match
        case Some(application) => Future.successful(Right(new AgentRegistrationApplicationRequest(
          agentRegistrationApplication = application,
          internalUserId = request.internalUserId,
          groupId = request.groupId,
          request = request.request
        )))
        case None => createNewApplication()
      }


  private def createNewApplication[A]()(using request: AuthorisedRequest[A]): Future[Either[Result, AgentRegistrationApplicationRequest[A]]] =
    enrolmentStoreProxyConnector
      .queryEnrolmentsAllocatedToGroup(request.groupId)
      .map(_.exists(e => e.service === appConfig.hmrcAsAgentEnrolment.key && e.state === "Activated"))
      .flatMap { isHmrcAsAgentEnrolmentAllocatedToGroup =>
        if isHmrcAsAgentEnrolmentAllocatedToGroup then
          val redirectUrl: String = appConfig.taxAndSchemeManagementToSelfServeAssignmentOfAsaEnrolment
          logger.info(s"Request cannot be served by this microservice as ${appConfig.hmrcAsAgentEnrolment} is assigned to group, therefore redirecting user to taxAndSchemeManagementToSelfServeAssignmentOfAsaEnrolment ($redirectUrl)")
          Future.successful(Left(Redirect(redirectUrl)))
        else
          applicationService
            .upsertNewApplication()
            .map(agentRegistrationApplication => Right(AgentRegistrationApplicationRequest[A](
                agentRegistrationApplication = agentRegistrationApplication,
                internalUserId = request.internalUserId,
                groupId = request.groupId,
                request = request.request
            )))
      }

  override protected def executionContext: ExecutionContext = ec
