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
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.connectors.CompaniesHouseApiProxyConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

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
      condition =
        implicit request =>
          if (request.agentApplication.businessType === BusinessType.SoleTrader)
            request
              .agentApplication
              .asSoleTraderApplication
              .deceasedCheck === EntityCheckResult.Pass
          else
            request.agentApplication
              .refusalToDealWithCheck === EntityCheckResult.Pass,
      resultWhenConditionNotMet =
        implicit request =>
          if (request.agentApplication.businessType === BusinessType.SoleTrader) {
            logger.warn("Deceased verification has not been completed. Redirecting to deceased check.")
            Redirect(AppRoutes.apply.internal.DeceasedController.check())
          }
          else
            logger.warn("Refusal to deal with verification has not been completed. Redirecting to refusal to deal with check.")
            Redirect(AppRoutes.apply.internal.RefusalToDealWithController.check())
    )
    .ensure(
      condition =
        _.agentApplication match
          case a: AgentApplication.IsIncorporated => a.companyStatusCheckResult =!= EntityCheckResult.Pass
          case _ => true,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Company status already done or not needed. Redirecting to task list page.")
          Redirect(nextPage)
    )
    .async:
      implicit request =>
        request.agentApplication match
          case a: AgentApplication.IsIncorporated => doCompanyStatusCheck(a)
          case _: AgentApplication.IsNotIncorporated =>
            Future.failed[Result](
              new RuntimeException(s"Unexpected application type: ${getClass.getSimpleName}.")
            )



      private def doCompanyStatusCheck(agentApplication: AgentApplication.IsIncorporated)(using request: AuthorisedRequest[?]): Future[Result] =

        for
          companyStatusCheckResult <- companiesHouseApiProxyConnector
            .getCompanyHouseStatus(agentApplication.dontCallMe_getCompanyProfile..companyNumber)
            .map(_.toEntityCheckResult)
          _ <- agentApplicationService
          _ <- agentApplicationService
      .upsert:
        agentApplication match
          case a: AgentApplicationLimitedCompany => a.modify(_.companyStatusCheckResult).setTo(Some(companyStatusCheckResult))
          case a: AgentApplicationLimitedPartnership => a.modify(_.companyStatusCheckResult).setTo(Some(companyStatusCheckResult))
          case a: AgentApplicationLlp => a.modify(_.companyStatusCheckResult).setTo(Some(companyStatusCheckResult))
          case a: AgentApplicationScottishLimitedPartnership => a.modify(_.companyStatusCheckResult).setTo(Some(companyStatusCheckResult))
  yield companyStatusCheckResult match          case EntityCheckResult.Pass => Redirect(nextPage)
          case EntityCheckResult.Fail => Redirect(failedCheckPage)
          case EntityCheckResult.NotChecked => throwServerErrorException("Companies House status check resulted in NotChecked")

  private def failedCheckPage = AppRoutes.apply.entitycheckfailed.CanNotRegisterCompanyOrPartnershipController.show
  private def nextPage: Call = AppRoutes.apply.TaskListController.show
