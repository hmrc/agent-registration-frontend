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
import java.time.temporal.ChronoUnit
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
      val numberOfDaysToSubmitApplication: Int = appConfig.daysToSubmitApplication.toDays.toInt

      /**
       * Because the number of grace period days is stored as config in agent-registration, we can reverse engineer the
       * Instant [[request.agentApplication.postExpiryGracePeriodEndsAt]] saved into the Expired application by the
       * backend to derive this number of days. This is to avoid maintaining duplicating config values.
        */
      val numberOfDaysBetweenGracePeriodAndCreatedAt: Int =
        request
          .agentApplication
          .postExpiryGracePeriodEndsAt
          .map(i => ChronoUnit.DAYS.between(i, request.agentApplication.createdAt))
          .getOrElse(0L)
          .toInt
      Ok(view(
        numberOfDaysToSubmitApplication = numberOfDaysToSubmitApplication,
        numberOfDaysGracePeriod = numberOfDaysBetweenGracePeriodAndCreatedAt - numberOfDaysToSubmitApplication
      ))
  def submit: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        agentApplicationService
          .deleteAndStartAgain()
          .map: _ =>
            Redirect(AppRoutes.apply.expired.UnsubmittedApplicationDeletedController.show)
