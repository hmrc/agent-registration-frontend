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
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.connectors.CompaniesHouseApiProxyConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistration.shared.CompanyStatusCheckResult

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

  private def nextPage: Call = AppRoutes.apply.TaskListController.show

  def companyStatusCheck(): Action[AnyContent] = actions
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
        _.agentApplication
          .entityCheckResult
          .isDefined,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Entity verification has not been done. Redirecting to entity check.")
          Redirect(AppRoutes.apply.internal.EntityCheckController.entityCheck())
    )
    .ensure(
      condition =
        _.agentApplication match
          case a: IsIncorporated => a.companyStatusCheckResult.isEmpty
          case _ => true,
      resultWhenConditionNotMet =
        implicit request =>
          logger.info("Company status already done or not needed. Redirecting to task list page.")
          Redirect(nextPage)
    )
    .async:
      implicit request =>
        request.agentApplication match
          case a: IsIncorporated => doCompanyStatusCheck(a)
          case _ =>
            Future.failed[Result](
              new RuntimeException(s"Unexpected application type: ${getClass.getSimpleName}.")
            )

  private def doCompanyStatusCheck(agentApplication: IsIncorporated)(using request: AuthorisedRequest[?]): Future[Result] =
    for
      companyStatusCheckResult: CompanyStatusCheckResult <- companiesHouseApiProxyConnector
        .getCompanyHouseStatus(agentApplication.dontCallMe_getCompanyProfile.companyNumber)
        .map(_.toCompanyStatusCheckResult)
      _ <- agentApplicationService
        .upsert:
          agentApplication match
            case a: AgentApplicationLimitedCompany => a.modify(_.companyStatusCheckResult).setTo(Some(companyStatusCheckResult))
            case a: AgentApplicationLimitedPartnership => a.modify(_.companyStatusCheckResult).setTo(Some(companyStatusCheckResult))
            case a: AgentApplicationLlp => a.modify(_.companyStatusCheckResult).setTo(Some(companyStatusCheckResult))
            case a: AgentApplicationScottishLimitedPartnership => a.modify(_.companyStatusCheckResult).setTo(Some(companyStatusCheckResult))
    yield companyStatusCheckResult match
      case CompanyStatusCheckResult.Allow => Redirect(nextPage)
      case CompanyStatusCheckResult.Block => Redirect(AppRoutes.apply.CompanyStatusBlockController.showBlockedPage)
