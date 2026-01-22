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

package uk.gov.hmrc.agentregistrationfrontend.individual.action

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.Results.Redirect
import play.api.mvc.ActionRefiner
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Request
import play.api.mvc.Result
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.Nino as ModelNino
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.shared.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.shared.action.MergeFormValue
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.views.ErrorResults
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.hc

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class IndividualAuthorisedWithIdentifiersRequest[A](
  override val internalUserId: InternalUserId,
  override val request: Request[A],
  override val credentials: Credentials,
  val nino: Option[ModelNino],
  val saUtr: Option[SaUtr]
)
extends IndividualAuthorisedRequest[A](
  internalUserId = internalUserId,
  request = request,
  credentials = credentials
)

object IndividualAuthorisedWithIdentifiersRequest:

  given [T, A]: MergeFormValue[IndividualAuthorisedWithIdentifiersRequest[A], T] =
    (
      r: IndividualAuthorisedWithIdentifiersRequest[A],
      t: T
    ) =>
      new IndividualAuthorisedWithIdentifiersRequest[A](
        r.internalUserId,
        r.request,
        r.credentials,
        r.nino,
        r.saUtr
      ) with FormValue[T]:
        val formValue: T = t

@Singleton
class IndividualAuthorisedWithIdentifiersAction @Inject() (
  af: AuthorisedFunctions,
  errorResults: ErrorResults,
  appConfig: AppConfig,
  cc: MessagesControllerComponents
)
extends ActionRefiner[Request, IndividualAuthorisedWithIdentifiersRequest]
with RequestAwareLogging:

  override protected def executionContext: ExecutionContext = cc.executionContext

  override protected def refine[A](request: Request[A]): Future[Either[Result, IndividualAuthorisedWithIdentifiersRequest[A]]] =
    given r: Request[A] = request

    af.authorised(
      AuthProviders(GovernmentGateway)
        and AffinityGroup.Individual
    ).retrieve(
      Retrievals.allEnrolments
        and Retrievals.internalId
        and Retrievals.credentials
    ).apply:
      case allEnrolments ~ maybeInternalId ~ credentials =>
        Future.successful(Right(new IndividualAuthorisedWithIdentifiersRequest(
          internalUserId = maybeInternalId
            .map(InternalUserId.apply)
            .getOrElse(Errors.throwServerErrorException("Retrievals for internalId is missing")),
          request = request,
          credentials = credentials.getOrElse(Errors.throwServerErrorException("Retrievals for credentials is missing")),
          nino = getNino(allEnrolments),
          saUtr = getUtr(allEnrolments)
        )))
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

  private def getIdentifierForEnrolmentKey(
    enrolmentKey: String,
    identifierKey: String
  )(using enrolments: Enrolments): Option[EnrolmentIdentifier] =
    for {
      enrolment <- enrolments.getEnrolment(enrolmentKey)
      identifier <- enrolment.getIdentifier(identifierKey)
    } yield identifier

  private def getNino(enrolments: Enrolments): Option[ModelNino] =
    given enr: Enrolments = enrolments
    val hmrcPtEnrolmentKey = "HMRC-PT"
    val hmrcNiEnrolmentKey = "HMRC-NI"
    val ninoIdentifierKey = "NINO"

    getIdentifierForEnrolmentKey(hmrcPtEnrolmentKey, ninoIdentifierKey)
      .orElse(getIdentifierForEnrolmentKey(hmrcNiEnrolmentKey, ninoIdentifierKey))
      .map(x => ModelNino(x.value))

  private def getUtr(enrolments: Enrolments): Option[SaUtr] =
    given enr: Enrolments = enrolments
    val hmrcPtEnrolmentKey = "IR-SA"
    val utrIdentifierKey = "UTR"

    getIdentifierForEnrolmentKey(hmrcPtEnrolmentKey, utrIdentifierKey)
      .map(x => SaUtr(x.value))

  private given ExecutionContext = cc.executionContext
