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
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.checkfailed.UtrAlreadyInUsePage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UtrAlreadyInUseController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  utrAlreadyInUsePage: UtrAlreadyInUsePage
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] =
    actions.getApplication
      .ensure(
        condition =
          implicit request =>
            request.agentApplication.applicationState === ApplicationState.Started,
        resultWhenConditionNotMet =
          implicit request =>
            logger.info(s"Duplicate UTR page requested but it is only for users who have status 'Started', the current application state is ${request.agentApplication.applicationState}, redirecting to landing page")
            Redirect(AppRoutes.apply.AgentApplicationController.landing)
      ):
        implicit request =>
          Ok(utrAlreadyInUsePage())
