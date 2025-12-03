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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails.internal

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedWithIdentifiersRequest
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.connectors.llp.CitizenDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.llp.MemberProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class InitiateMemberProvideDetailsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  memberProvideDetailsService: MemberProvideDetailsService,
  agentApplicationService: AgentApplicationService,
  citizenDetailsConnector: CitizenDetailsConnector
)
extends FrontendController(mcc, actions):

  /** This endpoint is called by resolve upon successful login.
    */
  def initiateMemberProvideDetails(
    linkId: LinkId
  ): Action[AnyContent] = actions
    .Member
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
              memberProvideDetailsService
                .findByApplicationId(agentApplication.agentApplicationId)
                .flatMap:
                  case None =>
                    for {
                      memberProvidedDetails <- createMemberProvidedDetailsFor(agentApplication.agentApplicationId)
                      _ <- memberProvideDetailsService.upsert(memberProvidedDetails)
                    } yield Redirect(nextEndpoint).addToSession(agentApplication.agentApplicationId)

                  case Some(memberProvideDetails) =>
                    logger.info("Member provided details already exists, redirecting to member name page")
                    Future.successful(Redirect(nextEndpoint).addToSession(memberProvideDetails.agentApplicationId))

            case None =>
              logger.info(s"Application for linkId $linkId not found, redirecting to $applicationGenericExitPageUrl")
              Future.successful(Redirect(applicationGenericExitPageUrl))

  private def createMemberProvidedDetailsFor(
    applicationId: AgentApplicationId
  )(using request: IndividualAuthorisedWithIdentifiersRequest[AnyContent]): Future[MemberProvidedDetails] =
    (request.nino, request.saUtr) match
      case (Some(nino), None) =>
        citizenDetailsConnector
          .getCitizenDetails(nino)
          .map { citizenDetails =>
            memberProvideDetailsService.createNewMemberProvidedDetails(
              internalUserId = request.internalUserId,
              agentApplicationId = applicationId,
              nino = request.nino,
              saUtr = citizenDetails.saUtr
            )
          }

      case _ =>
        Future.successful(
          memberProvideDetailsService.createNewMemberProvidedDetails(
            internalUserId = request.internalUserId,
            agentApplicationId = applicationId,
            nino = request.nino,
            saUtr = request.saUtr
          )
        )
