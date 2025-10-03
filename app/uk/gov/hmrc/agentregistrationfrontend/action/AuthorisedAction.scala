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

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.Results.*
import play.api.mvc.*
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.hc
import uk.gov.hmrc.agentregistrationfrontend.views.ErrorResults
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class AuthorisedRequest[A](
  val internalUserId: InternalUserId,
  val groupId: GroupId,
  val request: Request[A]
)
extends WrappedRequest[A](request)

object AuthorisedRequest:

  given [T, A]: MergeFormValue[AuthorisedRequest[A], T] =
    (
      r: AuthorisedRequest[A],
      t: T
    ) =>
      new AuthorisedRequest[A](
        r.internalUserId,
        r.groupId,
        r.request
      ) with FormValue[T]:
        val formValue: T = t

@Singleton
class AuthorisedAction @Inject() (
  af: AuthorisedFunctions,
  errorResults: ErrorResults,
  appConfig: AppConfig,
  cc: MessagesControllerComponents
)
extends ActionRefiner[Request, AuthorisedRequest]
with RequestAwareLogging:

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] =
    given r: Request[A] = request

    af.authorised(
      AuthProviders(GovernmentGateway)
        and AffinityGroup.Agent
    ).retrieve(
      Retrievals.allEnrolments
        and Retrievals.groupIdentifier
        and Retrievals.credentialRole
        and Retrievals.internalId
    ).apply:
      case allEnrolments ~ maybeGroupIdentifier ~ credentialRole ~ maybeInternalId =>
        if isUnsupportedCredentialRole(credentialRole) then
          logger.info(s"Unauthorised because of 'UnsupportedCredentialRole'")
          Future.successful(Left(errorResults.unauthorised(message = "UnsupportedCredentialRole")))
        else if isHmrcAsAgentEnrolmentAssignedToUser(allEnrolments) then
          val redirectUrl: String = appConfig.asaDashboardUrl
          logger.info(s"Enrolment ${appConfig.hmrcAsAgentEnrolment} is assigned to user, redirecting to ASA Dashboard ($redirectUrl)")
          Future.successful(Left(Redirect(redirectUrl)))
        else
          Future.successful(Right(new AuthorisedRequest(
            internalUserId = maybeInternalId
              .map(InternalUserId.apply)
              .getOrElse(Errors.throwServerErrorException("Retrievals for internalId is missing")),
            groupId = maybeGroupIdentifier
              .map(GroupId.apply)
              .getOrElse(Errors.throwServerErrorException("Retrievals for group identifier is missing")),
            request = request
          )))
    .recoverWith:
      case _: NoActiveSession =>
        logger.info(s"Unauthorised because of 'NoActiveSession', redirecting to sign in page")
        Future.successful(Left(Redirect(
          url = appConfig.signInUri(uri"""${appConfig.thisFrontendBaseUrl + request.uri}""").toString()
        )))
      case e: UnsupportedAuthProvider =>
        logger.info(s"Unauthorised because of '${e.reason}', ${e.toString}")
        Future.successful(Left(
          errorResults.unauthorised(
            message = e.reason
          )
        ))
      case e: UnsupportedAffinityGroup =>
        logger.info(s"Unauthorised because of '${e.reason}', ${e.toString}")
        Future.successful(Left(
          errorResults.unauthorised(
            message = e.reason
          )
        ))
      case e: UnsupportedCredentialRole =>
        logger.info(s"Unauthorised because of '${e.reason}', ${e.toString}")
        Future.successful(Left(
          errorResults.unauthorised(
            message = e.reason
          )
        ))
      case e: AuthorisationException =>
        logger.info(s"Unauthorised because of '${e.reason}', ${e.toString}")
        Future.successful(Left(
          errorResults.unauthorised(
            message = e.toString
          )
        ))

  override protected def executionContext: ExecutionContext = cc.executionContext

  private def isHmrcAsAgentEnrolmentAssignedToUser[A](allEnrolments: Enrolments) = allEnrolments
    .getEnrolment(appConfig.hmrcAsAgentEnrolment.key)
    .exists(_.isActivated)

  private def isUnsupportedCredentialRole[A](maybeCredentialRole: Option[CredentialRole])(using request: RequestHeader) =
    @nowarn
    val supportedCredentialRoles: Set[CredentialRole] = Set(User, Admin)
    val credentialRole: CredentialRole = maybeCredentialRole.getOrElse(Errors.throwServerErrorException("Retrievals for CredentialRole is missing"))
    !supportedCredentialRoles.contains(credentialRole)

  @nowarn
  private val credentialRoleAdmin: Predicate = User.or(Admin) // Admin and User are equivalent, Admin is deprecated

  private given ExecutionContext = cc.executionContext
