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
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=

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
          logger.debug("No Companies House check required for non-incorporated business types, redirecting to task list.")
          Redirect(AppRoutes.apply.TaskListController.show)
    )
    .ensure(
      condition =
        _.agentApplication match
          case a: AgentApplication.IsIncorporated => a.isCompanyStatusCheckRequired
          case _: AgentApplication.IsNotIncorporated => false,
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
        .getCompanyHouseStatus(agentApplication.dontCallMe_getCompanyProfile.companyNumber)
        .map(_.toCheckResult)
      _ <- agentApplicationService
        .upsert:
          agentApplication match
            case a: AgentApplicationLimitedCompany => a.modify(_.companyStatusCheckResult).setTo(Some(companyStatusCheckResult))
            case a: AgentApplicationLimitedPartnership => a.modify(_.companyStatusCheckResult).setTo(Some(companyStatusCheckResult))
            case a: AgentApplicationLlp => a.modify(_.companyStatusCheckResult).setTo(Some(companyStatusCheckResult))
            case a: AgentApplicationScottishLimitedPartnership => a.modify(_.companyStatusCheckResult).setTo(Some(companyStatusCheckResult))
    yield companyStatusCheckResult match
      case CheckResult.Pass => Redirect(nextPage)
      case CheckResult.Fail => Redirect(failedCheckPage)

  private def failedCheckPage = AppRoutes.apply.checkfailed.CanNotRegisterCompanyOrPartnershipController.show
  private def nextPage: Call = AppRoutes.apply.TaskListController.show

  extension (agentApplication: AgentApplication)

    private def isIncorporated: Boolean =
      agentApplication match
        case _: AgentApplication.IsIncorporated => true
        case _: AgentApplication.IsNotIncorporated => false

  extension (agentApplication: AgentApplication.IsIncorporated)
    private def isCompanyStatusCheckRequired: Boolean = agentApplication.companyStatusCheck =!= Some(CheckResult.Pass)
