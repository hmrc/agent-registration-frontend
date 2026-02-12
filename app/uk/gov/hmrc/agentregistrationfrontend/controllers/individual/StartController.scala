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
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.StartPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  startPage: StartPage,
  applicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  def start(linkId: LinkId): Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        val genericExitPageUrl: String = AppRoutes.apply.AgentApplicationController.genericExitPage.url
        applicationService.find(linkId).map {
          case Some(app) if app.hasFinished =>
            logger.info(s"Individual details can no longer be provided on this link id, the application for linkId $linkId has already finished, redirecting to $genericExitPageUrl")
            Redirect(genericExitPageUrl)
          case Some(app) => Ok(startPage(linkId: LinkId))
          case None =>
            logger.info(s"Application for linkId $linkId not found, redirecting to $genericExitPageUrl")
            Redirect(genericExitPageUrl)
        }
