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
import play.api.mvc.Call
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.action.MergeFormValue
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.llp.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class IndividualProvideDetailsRequest[A](
  val individualProvidedDetails: IndividualProvidedDetails,
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
    requirement = individualProvidedDetails.internalUserId === internalUserId,
    message =
      s"Sanity Check: InternalUserId from the request (${internalUserId.value}) must match the provided details " +
        s"retrieved from backend (${individualProvidedDetails.internalUserId.value}) (this should never happen)"
  )(using this)

object IndividualProvideDetailsRequest:

  given [B, T]: MergeFormValue[IndividualProvideDetailsRequest[B], T] =
    (
      r: IndividualProvideDetailsRequest[B],
      t: T
    ) =>
      new IndividualProvideDetailsRequest[B](
        individualProvidedDetails = r.individualProvidedDetails,
        internalUserId = r.internalUserId,
        request = r,
        credentials = r.credentials
      ) with FormValue[T]:
        override val formValue: T = t

@Singleton
class ProvideDetailsAction @Inject() (
  individualProvideDetailsService: IndividualProvideDetailsService,
  applicationService: AgentApplicationService
)(using ec: ExecutionContext)
extends ActionRefiner[IndividualAuthorisedRequest, IndividualProvideDetailsRequest]
with RequestAwareLogging:

  override protected def executionContext: ExecutionContext = ec

  override protected def refine[A](request: IndividualAuthorisedRequest[A]): Future[Either[Result, IndividualProvideDetailsRequest[A]]] =
    given r: IndividualAuthorisedRequest[A] = request

    val mpdGenericExitPage: Call = AppRoutes.providedetails.ExitController.genericExitPage
    val appGenericExitPageUrl: Call = AppRoutes.apply.AgentApplicationController.genericExitPage
    val multipleMpd: Call = AppRoutes.providedetails.ExitController.multipleProvidedDetailsPage
    def initiateMpd(
      linkId: LinkId
    ): Call = AppRoutes.providedetails.internal.InitiateIndividualProvideDetailsController.initiateIndividualProvideDetails(linkId = linkId)

    request.readAgentApplicationId match
      case None =>
        individualProvideDetailsService
          .findAll()
          .map:
            case Nil =>
              logger.info(s"Missing agentApplicationIn in session. Recovering failed. Individual provided details not found, redirecting to ${mpdGenericExitPage.url}")
              Left(Redirect(mpdGenericExitPage))
            case individualProvidedDetails :: Nil =>
              logger.info(s"Missing agentApplicationIn in session. Recovering success. One individual provided details found, redirecting to next page")
              Right(new IndividualProvideDetailsRequest(
                individualProvidedDetails = individualProvidedDetails,
                internalUserId = request.internalUserId,
                request = request.request,
                credentials = request.credentials
              ))
            case _ =>
              logger.info(s"Missing agentApplicationIn in session. Recovering failed. Multiple individual provided details found, redirecting to ${multipleMpd.url}")
              Left(Redirect(multipleMpd))

      case Some(agentApplicationId) =>
        individualProvideDetailsService
          .findByApplicationId(agentApplicationId)
          .flatMap:
            case Some(individualProvidedDetails) =>
              Future.successful(Right(new IndividualProvideDetailsRequest(
                individualProvidedDetails = individualProvidedDetails,
                internalUserId = request.internalUserId,
                request = request.request,
                credentials = request.credentials
              )))
            case None =>
              applicationService
                .find(agentApplicationId)
                .map:
                  case Some(app) if app.hasFinished =>
                    val initiateMpdUrl: Call = initiateMpd(app.linkId)
                    logger.info(s"Missing individual provided details in DB. Recovering success. Recovered  LinkId: ${app.linkId} for user:${app.internalUserId} and redirecting to initiate journey ${initiateMpdUrl.url}.")
                    Left(Redirect(initiateMpdUrl))
                  case Some(app) =>
                    logger.warn(s"Missing individual provided details in DB. Recovering failed. Application ${app.agentApplicationId} has not finished, redirecting to ${appGenericExitPageUrl.url}")
                    Left(Redirect(appGenericExitPageUrl))
                  case None =>
                    logger.info(s"Missing individual provided details in DB. Recovering failed. Agent application not found, redirecting to ${appGenericExitPageUrl.url}")
                    Left(Redirect(appGenericExitPageUrl))
