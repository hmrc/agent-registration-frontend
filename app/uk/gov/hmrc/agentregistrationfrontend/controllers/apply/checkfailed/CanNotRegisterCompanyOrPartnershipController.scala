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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.checkfailed

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.companyStatusCheck
import uk.gov.hmrc.agentregistration.shared.CheckResult
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.checkfailed.CompanyStatusBlockPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanNotRegisterCompanyOrPartnershipController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  companyStatusBlockPage: CompanyStatusBlockPage
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] =
    actions
      .getApplicationInProgress
      .ensure4(
        condition =
          _.agentApplication match
            case a: AgentApplication.IsIncorporated => a.companyStatusCheck === Some(CheckResult.Fail)
            case a: AgentApplication.IsNotIncorporated => false,
        resultWhenConditionNotMet =
          implicit request =>
            logger.warn("Companies house status check has not been blocked. Redirecting to company status check.")
            Redirect(AppRoutes.apply.internal.CompaniesHouseStatusController.check())
      ):
        implicit request =>
          Ok(companyStatusBlockPage())
