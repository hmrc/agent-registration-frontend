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
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedWithIdentifiersRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.services.llp.IndividualProvideDetailsService

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class InitiateIndividualProvideDetailsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  individualProvideDetailsService: IndividualProvideDetailsService,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  /** This endpoint is called by resolve upon successful login.
    */
  def initiateIndividualProvideDetails(
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
              // TODO: find IndividualProvidedDetails by application id - currently uses internalUserId from the request
              // do we relax and use application id only to return a list of IndividualProvidedDetails and pick the correct one??
              // if we have internalUserId present in the repo we can ensure the user is the owner of the provided details
              // if there is no internalUserId match we could search for records with application id and Precreated status to attempt an auto-match
              // based on CitizenDetails name and the name the record was precreated with
              // if we find no auto-match we need to prompt the user for their name to do another search, until then we should only use AuthorisedAction
              // upon name match or auto match is confirmed by user (is this you?) we can then hydrate teh record with identifiers and internalUserId
              individualProvideDetailsService
                .findByApplicationId(agentApplication.agentApplicationId)
                .flatMap:
                  case None =>
                    // TODO: this would be the best place to find any "Precreated" provided details that matches the application id and another identifier
                    // do we use the name from citizens details to attempt an auto-match against precreated individual records before prompting for a name??
                    logger.info(s"IndividualProvidedDetails for linkId $linkId not found, redirecting to $applicationGenericExitPageUrl")
                    Future.successful(Redirect(applicationGenericExitPageUrl))

                  case Some(individualProvidedDetails) =>
                    logger.info("Individual provided details already exists, redirecting to individual name page")
                    Future.successful(Redirect(nextEndpoint).addToSession(individualProvidedDetails.agentApplicationId))

            case None =>
              logger.info(s"Application for linkId $linkId not found, redirecting to $applicationGenericExitPageUrl")
              Future.successful(Redirect(applicationGenericExitPageUrl))
