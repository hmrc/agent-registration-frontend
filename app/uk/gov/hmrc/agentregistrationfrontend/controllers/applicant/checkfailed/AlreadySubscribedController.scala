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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.checkfailed

import play.api.mvc.*
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.checkfailed.AlreadySubscribedPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlreadySubscribedController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  alreadySubscribedPage: AlreadySubscribedPage
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] =
    actions
      .getApplicationInProgress
      .ensure(
        condition =
          _.agentApplication
            .isDuplicateAsa.get,
        resultWhenConditionNotMet =
          implicit request =>
            logger.warn("Duplicate ASA check has not failed. Redirecting to duplicate ASA check.")
            Redirect(AppRoutes.apply.internal.DuplicateAsaCheckController.check())
      ):
        implicit request =>
          Ok(alreadySubscribedPage())
