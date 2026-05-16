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
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.checkfailed.UnableToConfirmBusinessDetailsPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnableToConfirmBusinessDetailsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: UnableToConfirmBusinessDetailsPage
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] =
    actions
      .getApplicationInProgress
      .ensure(
        condition = !_.agentApplication.isGrsDataReceived,
        resultWhenConditionNotMet =
          implicit request =>
            logger.warn("GRS data already received. Redirecting to verify entity.")
            Redirect(AppRoutes.apply.internal.RefusalToDealWithController.check())
      ):
        implicit request =>
          Ok(view())
