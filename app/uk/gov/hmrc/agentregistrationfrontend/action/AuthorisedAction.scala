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
import play.api.mvc.*
import play.api.mvc.Results.*
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.agentregistrationfrontend.model.Arn
import uk.gov.hmrc.agentregistrationfrontend.model.GroupId
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.hc
import uk.gov.hmrc.agentregistrationfrontend.util.SafeEquals.*
import uk.gov.hmrc.agentregistrationfrontend.views.ErrorResults
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AuthorisedAction @Inject() (
  af: AuthorisedFunctions,
  errorResults: ErrorResults,
  appConfig: AppConfig,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
  cc: MessagesControllerComponents
)
extends ActionRefiner[Request, Request]
with RequestAwareLogging:

  override protected def refine[A](request: Request[A]): Future[Either[Result, Request[A]]] =
    given r: Request[A] = request

    af.authorised(
      AuthProviders(GovernmentGateway)
        and AffinityGroup.Agent
//        and credentialRoleAdmin
    ).retrieve(
      Retrievals.allEnrolments
        and Retrievals.groupIdentifier
        and Retrievals.credentialRole
    ).apply:
      case allEnrolments ~ maybeGroupIdentifier ~ credentialRole =>

        println(credentialRole)

        val groupId: GroupId = maybeGroupIdentifier
          .map(GroupId.apply)
          .getOrElse(Errors.throwServerErrorException("Expected group identifier to be found"))

        val isHmrcAsAgentEnrolmentAssignedToUser: Boolean = allEnrolments
          .getEnrolment(hmrcAsAgentEnrolment.key)
          .exists(_.isActivated)

        if isHmrcAsAgentEnrolmentAssignedToUser then
          val redirectUrl: String = appConfig.asaDashboardUrl
          logger.info(s"Enrolment $hmrcAsAgentEnrolment is assigned to user, redirecting to ASA Dashboard ($redirectUrl)")
          Future.successful(Left(Redirect(redirectUrl)))
        else
          for
            enrolmentsInGroup <- enrolmentStoreProxyConnector
              .queryEnrolmentsAllocatedToGroup(
                groupId = groupId
              )
            isHmrcAsAgentEnrolmentAllocatedToGroup: Boolean = enrolmentsInGroup.exists(e =>
              e.service === hmrcAsAgentEnrolment.key && e.state === "Activated"
            )
          yield
            if isHmrcAsAgentEnrolmentAllocatedToGroup then
              val redirectUrl: String = appConfig.taxAndSchemeManagementToSelfServeAssignmentOfAsaEnrolment
              logger.info(s"Enrolment $hmrcAsAgentEnrolment is assigned to user, redirecting to taxAndSchemeManagementToSelfServeAssignmentOfAsaEnrolment ($redirectUrl)")
              Left(Redirect(redirectUrl))
            else
              Right(request)
    .recoverWith:
      case _: NoActiveSession =>
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

  private val hmrcAsAgentEnrolment: Enrolment = Enrolment(key = "HMRC-AS-AGENT")

  @nowarn
  private val credentialRoleAdmin: Predicate = User.or(Admin) // Admin and User are equivalent, Admin is deprecated
  override protected def executionContext: ExecutionContext = cc.executionContext
  private given ExecutionContext = cc.executionContext
