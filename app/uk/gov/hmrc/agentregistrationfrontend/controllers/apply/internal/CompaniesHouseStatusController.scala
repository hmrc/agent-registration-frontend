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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.internal

import com.softwaremill.quicklens.*
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.connectors.CompaniesHouseApiProxyConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompaniesHouseStatusController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  companiesHouseApiProxyConnector: CompaniesHouseApiProxyConnector,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  def check(): Action[AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      condition =
        _.agentApplication
          .isIncorporated,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("No Companies House check required for non-incorporated business types, redirecting to task list.")
          Redirect(AppRoutes.apply.TaskListController.show)
    )
    .ensure(
      condition = _.agentApplication.companyStatusCheckResult.forall(_ === EntityCheckResult.Fail),
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Company status check already completed successfully. Redirecting to task list.")
          Redirect(nextPage)
    )
    .async:
      implicit request =>
        val agentApplication = request.agentApplication

        for
          companyStatusCheckResult <- companiesHouseApiProxyConnector
            .getCompanyHouseStatus(agentApplication.getCompanyProfile.companyNumber)
            .map(_.toEntityCheckResult)
          _ <- agentApplicationService
            .upsert(agentApplication
              .modify(_.companyStatusCheckResult)
              .setTo(Some(companyStatusCheckResult)))
        yield companyStatusCheckResult match
          case EntityCheckResult.Pass => Redirect(nextPage)
          case EntityCheckResult.Fail => Redirect(failedCheckPage)

  private def failedCheckPage = AppRoutes.apply.entitycheckfailed.CanNotRegisterCompanyOrPartnershipController.show
  private def nextPage: Call = AppRoutes.apply.TaskListController.show
