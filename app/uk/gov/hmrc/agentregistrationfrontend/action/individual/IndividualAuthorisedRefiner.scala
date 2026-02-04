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

package uk.gov.hmrc.agentregistrationfrontend.action.individual

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.*
import play.api.mvc.Results.*
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistrationfrontend.action.RequestWithDataCt
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.hc
import uk.gov.hmrc.agentregistrationfrontend.util.UniqueTuple.AbsentIn
import uk.gov.hmrc.agentregistrationfrontend.views.ErrorResults
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class IndividualAuthorisedRefiner @Inject() (
  af: AuthorisedFunctions,
  errorResults: ErrorResults,
  appConfig: AppConfig
)(using ExecutionContext)
extends RequestAwareLogging:

  def refine[
    ContentType,
    Data <: Tuple
  ](
    request: RequestWithDataCt[ContentType, Data]
  )(
    using
    Credentials AbsentIn Data,
    InternalUserId AbsentIn Data
  ): Future[Either[Result, RequestWithDataCt[
    ContentType,
    InternalUserId *: Credentials *: Data
  ]]] =
    given RequestWithDataCt[ContentType, Data] = request

    af.authorised(
      AuthProviders(GovernmentGateway)
        and AffinityGroup.Individual
    ).retrieve(
      Retrievals.allEnrolments
        and Retrievals.internalId
        and Retrievals.credentials
    ).apply:
      case allEnrolments ~ maybeInternalId ~ maybeCredentials =>
        val internalUserId: InternalUserId = maybeInternalId.map(
          InternalUserId.apply
        ).getOrElse(Errors.throwServerErrorException("Retrievals for internalId is missing"))
        val credentials: Credentials = maybeCredentials.getOrElse(Errors.throwServerErrorException("Retrievals for credentials is missing"))
        Future.successful(Right(
          request
            .add(credentials)
            .add(internalUserId)
        ))
    .recoverWith:
      case _: NoActiveSession =>
        logger.info(s"Unauthorised because of 'NoActiveSession', redirecting to sign in page")
        Future.successful(Left(Redirect(
          url = appConfig.signInUri(uri"""${appConfig.thisFrontendBaseUrl + request.uri}""", AffinityGroup.Individual).toString()
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
      case e: AuthorisationException =>
        logger.info(s"Unauthorised because of '${e.reason}', ${e.toString}")
        Future.successful(Left(
          errorResults.unauthorised(
            message = e.toString
          )
        ))
