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

package uk.gov.hmrc.agentregistrationfrontend.applicant.controllers.checkfailed

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.CheckResult.Fail
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.shared.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.checkfailed.CanNotRegisterPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanNotRegisterController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  canNotRegisterPage: CanNotRegisterPage
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] =
    actions
      .Applicant
      .getApplicationInProgress
      .ensure(
        condition =
          _.agentApplication
            .refusalToDealWithCheckResult === Some(Fail),
        resultWhenConditionNotMet =
          implicit request =>
            logger.warn("Refusal to deal with has not failed. Redirecting to run refusal to deal with check.")
            Redirect(AppRoutes.apply.internal.RefusalToDealWithController.check())
      ):
        implicit request =>
          Ok(canNotRegisterPage(request
            .agentApplication
            .dontCallMe_getCompanyProfile
            .companyName))
