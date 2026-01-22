/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.individual.controllers.internal

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.llp.IndividualNino
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.llp.IndividualSaUtr
import uk.gov.hmrc.agentregistrationfrontend.individual.action.IndividualAuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.individual.action.IndividualAuthorisedWithIdentifiersRequest
import uk.gov.hmrc.agentregistrationfrontend.connectors.CitizenDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.services.llp.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.shared.action.Actions

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class InitiateIndividualProvideDetailsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  individualProvideDetailsService: IndividualProvideDetailsService,
  agentApplicationService: AgentApplicationService,
  citizenDetailsConnector: CitizenDetailsConnector
)
extends FrontendController(mcc, actions):

  /** This endpoint is called by resolve upon successful login.
    */
  def initiateMemberProvideDetails(
    linkId: LinkId
  ): Action[AnyContent] = actions
    .Individual
    .authorisedWithIdentifiers
    .async:
      implicit request: IndividualAuthorisedWithIdentifiersRequest[AnyContent] =>

        implicit val individualAuthorisedRequest: IndividualAuthorisedRequest[AnyContent] =
          new IndividualAuthorisedRequest[AnyContent](
            request.internalUserId,
            request.request,
            request.credentials
          )

        val nextEndpoint: Call = AppRoutes.providedetails.CompaniesHouseNameQueryController.show
        val applicationGenericExitPageUrl: String = AppRoutes.apply.AgentApplicationController.genericExitPage.url

        agentApplicationService.find(linkId)
          .flatMap:
            case Some(agentApplication) =>
              individualProvideDetailsService
                .findByApplicationId(agentApplication.agentApplicationId)
                .flatMap:
                  case None =>
                    for {
                      memberProvidedDetails <- createMemberProvidedDetailsFor(agentApplication.agentApplicationId)
                      _ <- individualProvideDetailsService.upsert(memberProvidedDetails)
                    } yield Redirect(nextEndpoint).addToSession(agentApplication.agentApplicationId)

                  case Some(memberProvideDetails) =>
                    logger.info("Individual provided details already exists, redirecting to individual name page")
                    Future.successful(Redirect(nextEndpoint).addToSession(memberProvideDetails.agentApplicationId))

            case None =>
              logger.info(s"Application for linkId $linkId not found, redirecting to $applicationGenericExitPageUrl")
              Future.successful(Redirect(applicationGenericExitPageUrl))

  private def createMemberProvidedDetailsFor(
    applicationId: AgentApplicationId
  )(using request: IndividualAuthorisedWithIdentifiersRequest[AnyContent]): Future[IndividualProvidedDetails] =
    (request.nino, request.saUtr) match
      case (Some(nino), None) =>
        citizenDetailsConnector
          .getCitizenDetails(nino)
          .map { citizenDetails =>
            individualProvideDetailsService.createNewIndividualProvidedDetails(
              internalUserId = request.internalUserId,
              agentApplicationId = applicationId,
              memberNino = request.nino.map(IndividualNino.FromAuth.apply),
              memberSaUtr = citizenDetails.saUtr.map(IndividualSaUtr.FromCitizenDetails.apply),
              memberDateOfBirth = citizenDetails.dateOfBirth
            )
          }

      case _ =>
        Future.successful(
          individualProvideDetailsService.createNewIndividualProvidedDetails(
            internalUserId = request.internalUserId,
            agentApplicationId = applicationId,
            memberNino = request.nino.map(IndividualNino.FromAuth.apply),
            memberSaUtr = request.saUtr.map(IndividualSaUtr.FromAuth.apply)
          )
        )
