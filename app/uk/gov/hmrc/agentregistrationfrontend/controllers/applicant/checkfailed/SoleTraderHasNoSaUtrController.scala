/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.checkfailed

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsNotSoleTrader
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.checkfailed.SoleTraderHasNoSaUtrPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoleTraderHasNoSaUtrController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  soleTraderHasNoSaUtrPage: SoleTraderHasNoSaUtrPage
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] =
    actions.getApplication
      .ensure(
        condition =
          implicit request =>
            request.agentApplication match
              case a: AgentApplicationSoleTrader => a.businessDetails.isEmpty // having business details means SaUtr is present
              case _: IsNotSoleTrader => false,
        resultWhenConditionNotMet =
          implicit request =>
            logger.info(s"[SoleTraderHasNoUtrController] Agent application ref ${request.agentApplication.applicationReference.value} is not for a sole trader without a UTR, redirecting to task list will present user with correct status.")
            Redirect(AppRoutes.apply.TaskListController.show.url)
      ):
        implicit request =>
          Ok(soleTraderHasNoSaUtrPage())
