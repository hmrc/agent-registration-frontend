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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.AgentRegistrationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.LlpStartPage
import uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails.internal.routes as internalRoutes

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class StartController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  llpStartPage: LlpStartPage,
  placeholderStartPage: SimplePage,
  applicationService: AgentRegistrationService
)
extends FrontendController(mcc, actions):

  def start(linkId: LinkId): Action[AnyContent] = Action
    .async:
      implicit request: RequestHeader =>
        applicationService.findApplicationByLinkId(linkId).map {
          case Some(app) if app.hasFinished => startPageForApplicationType(app)
          case _ => Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)
        }

  // TODO: this method requires an auth action to ensure user is signed in correctly and has
  //  the application from the linkId provided within the request going forward
  def resolve(linkId: LinkId): Action[AnyContent] = actions.authorisedIndividual
    .async:
      implicit request =>
        applicationService.findApplicationByLinkId(linkId)
          .map:
            case Some(app) if app.hasFinished =>
              Redirect(internalRoutes
                .InitiateMemberProvideDetailsController.initiateMemberProvideDetails(linkId = linkId))
            case _ => Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)

  def startNoLink: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(placeholderStartPage(
      h1 = "Start page for no linkId",
      bodyText = Some(
        "Placeholder for the member provide details start page when no linkId is provided"
      )
    )))
  }

  // for now this returns only the llp start page template until we build the rest
  private def startPageForApplicationType(agentApplication: AgentApplication)(implicit request: RequestHeader): Result =
    agentApplication.businessType match
      case BusinessType.Partnership.LimitedLiabilityPartnership => Ok(llpStartPage(agentApplication.asLlpApplication))
      case _ =>
        Ok(placeholderStartPage(
          h1 = "Start page for unsupported application type",
          bodyText = Some("Placeholder for unbuilt start pages")
        ))
