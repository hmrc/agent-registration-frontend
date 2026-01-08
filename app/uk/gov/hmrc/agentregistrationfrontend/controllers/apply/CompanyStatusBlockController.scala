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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.CompanyStatusCheckResult.Block
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.CompanyStatusBlockPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanyStatusBlockController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  companyStatusBlockPage: CompanyStatusBlockPage
)
extends FrontendController(mcc, actions):

  val baseAction = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      condition =
        _.agentApplication
          .companyStatusCheckResult
          .exists(_ === Block),
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Entity verification has not been done. Redirecting to entity check.")
          Redirect(AppRoutes.apply.internal.CompaniesHouseStatusController.companyStatusCheck())
    )

  def showBlockedPage: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(companyStatusBlockPage())
