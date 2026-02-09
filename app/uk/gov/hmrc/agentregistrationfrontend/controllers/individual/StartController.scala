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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.LlpStartPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  llpStartPage: LlpStartPage,
  placeholderStartPage: SimplePage,
  applicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  def start(linkId: LinkId): Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        val genericExitPageUrl: String = AppRoutes.apply.AgentApplicationController.genericExitPage.url
        applicationService.find(linkId).map {
          case Some(app) if app.hasFinished => startPageForApplicationType(app)
          case Some(app) =>
            logger.warn(s"Application ${app.agentApplicationId} has not finished, redirecting to $genericExitPageUrl.")
            Redirect(genericExitPageUrl)
          case None =>
            logger.info(s"Application for linkId $linkId not found, redirecting to $genericExitPageUrl")
            Redirect(genericExitPageUrl)
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
