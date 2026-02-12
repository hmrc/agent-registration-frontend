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

import play.api.mvc.Call
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistrationfrontend.action.Actions.RequestWithData
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.UniqueTuple.AbsentIn

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class IndividualProvideDetailsRefiner @Inject() (
  individualProvideDetailsService: IndividualProvideDetailsService,
  applicationService: AgentApplicationService
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  def refineIntoRequestWithIndividualProvidedDetails[Data <: Tuple](
    request: RequestWithData[Data]
  )(
    using IndividualProvidedDetailsToBeDeleted AbsentIn Data
  ): Future[Either[Result, RequestWithData[IndividualProvidedDetailsToBeDeleted *: Data]]] =
    given RequestWithData[Data] = request

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
              Right(request.add(individualProvidedDetails))
            case _ =>
              logger.info(s"Missing agentApplicationIn in session. Recovering failed. Multiple individual provided details found, redirecting to ${multipleMpd.url}")
              Left(Redirect(multipleMpd))

      case Some(agentApplicationId) =>
        individualProvideDetailsService
          .findByApplicationId(agentApplicationId)
          .flatMap:
            case Some(individualProvidedDetails) => Future.successful(Right(request.add(individualProvidedDetails)))
            case None =>
              applicationService
                .find(agentApplicationId)
                .map:
                  case Some(app) if app.hasFinished =>
                    logger.warn(s"Application ${app.agentApplicationId} has finished, no longer able to provide details, redirecting to ${appGenericExitPageUrl.url}")
                    Left(Redirect(appGenericExitPageUrl))
                  case Some(app) =>
                    val initiateMpdUrl: Call = initiateMpd(app.linkId)
                    logger.info(s"Missing individual provided details in DB. Recovering success. Recovered  LinkId: ${app.linkId} for user:${app.internalUserId} and redirecting to initiate journey ${initiateMpdUrl.url}.")
                    Left(Redirect(initiateMpdUrl))
                  case None =>
                    logger.info(s"Missing individual provided details in DB. Recovering failed. Agent application not found, redirecting to ${appGenericExitPageUrl.url}")
                    Left(Redirect(appGenericExitPageUrl))
