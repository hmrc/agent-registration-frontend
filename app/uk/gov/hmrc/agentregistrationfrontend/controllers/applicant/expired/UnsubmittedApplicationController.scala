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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.expired

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.expired.UnsubmittedApplicationPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnsubmittedApplicationController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: UnsubmittedApplicationPage,
  agentApplicationService: AgentApplicationService,
  appConfig: AppConfig
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[DataWithApplication] = actions
    .getApplication
    .ensure(
      condition =
        implicit request =>
          request
            .agentApplication
            .applicationState === ApplicationState.Expired,
      implicit request =>
        logger.warn(s"Cannot view expired page because application state is ${request.agentApplication.applicationState.toString} which is not the Expired state")
        Redirect(AppRoutes.apply.AgentApplicationController.landing)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        numberOfDaysToSubmitApplication = appConfig.daysToSubmitApplication.toDays.toInt,
        numberOfDaysGracePeriod = 45 // this is stored in agent-registration config, could we calc from application's postGracePeriodExpiry date instead?
      ))

  def submit: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        agentApplicationService
          .deleteAndStartAgain()
          .map: _ =>
            Redirect(AppRoutes.apply.expired.UnsubmittedApplicationDeletedController.show)
