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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.entitycheckfailed

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.EntityCheckResult.Fail
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.entitycheckfailed.CanNotConfirmIdentityPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanNotConfirmIdentityController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  canNotConfirmIdentityPage: CanNotConfirmIdentityPage
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] =
    actions
      .Applicant
      .getApplicationInProgress
      .ensure(
        condition =
          _.agentApplication.businessType match
            case a: AgentApplication.IsIncorporated => a.companyStatusCheckResult.exists(_ === CompanyStatusCheckResult.Block)
            case _ => false,
        resultWhenConditionNotMet =
          implicit request =>
            logger.warn("Deceased check has not failed. Redirecting to deceased check.")
            Redirect(AppRoutes.apply.internal.DeceasedController.check())
      ):
        implicit request =>
          Ok(canNotConfirmIdentityPage())
