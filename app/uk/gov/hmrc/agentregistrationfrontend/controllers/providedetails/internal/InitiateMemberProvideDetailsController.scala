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
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails.routes
import uk.gov.hmrc.agentregistrationfrontend.services.AgentRegistrationService
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
  agentRegistrationService: AgentRegistrationService
)
extends FrontendController(mcc, actions):

  /** This endpoint is called by resolve upon successful login.
    */
  def initiateMemberProvideDetails(
    linkId: LinkId
  ): Action[AnyContent] = actions.authorisedIndividual
    .async:
      implicit request =>

        val nextEndpoint: Call = routes.LlpMemberNameController.show

        agentRegistrationService.findApplicationByLinkId(linkId).flatMap:
          case Some(agentApplication) =>
            memberProvideDetailsService
              .findByApplicationId(agentApplication.agentApplicationId)
              .flatMap {
                case None =>
                  memberProvideDetailsService
                    .upsert(memberProvideDetailsService
                      .createNewMemberProvidedDetails(agentApplication.agentApplicationId))
                    .map(_ => Redirect(nextEndpoint).addToSession(agentApplication.agentApplicationId))

                case Some(memberProvideDetails) =>
                  // TODO WG - fix the routing to the correct page depending on the status of the provided details
                  logger.info("Member provided details already exists, redirecting to member name page")
                  Future.successful(Redirect(nextEndpoint).addToSession(memberProvideDetails.agentApplicationId))
              }

          case None =>
            logger.info(s"Application does not exist for provided linkId: $linkId")
            // TODO - TBC - confirm next page when no application exists for provided linkId
            Future.successful(Redirect(AppRoutes.providedetails.StartController.startNoLink))
