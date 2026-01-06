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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.agentdetails

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentDetails
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.agentdetails.CheckYourAnswersPage

@Singleton
class CheckYourAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: CheckYourAnswersPage
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      _.agentApplication.asLlpApplication.agentDetails.exists(_.isComplete),
      implicit request =>
        logger.warn("Because we don't have complete agent details we are redirecting to where data is missing")
        request.agentApplication.asLlpApplication.agentDetails match {
          case None => Redirect(AppRoutes.apply.agentdetails.AgentBusinessNameController.show)
          case Some(AgentDetails(_, None, _, _)) => Redirect(AppRoutes.apply.agentdetails.AgentTelephoneNumberController.show)
          case Some(AgentDetails(_, _, None, _)) => Redirect(AppRoutes.apply.agentdetails.AgentEmailAddressController.show)
          case Some(AgentDetails(_, _, _, _)) => Redirect(AppRoutes.apply.agentdetails.AgentCorrespondenceAddressController.show)
        }
    )

  def show: Action[AnyContent] = baseAction:
    implicit request => Ok(view())
