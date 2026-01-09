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
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefusalToDealWithController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  agentAssuranceConnector: AgentAssuranceConnector,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  def check(): Action[AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      condition =
        _.agentApplication
          .applicationState === ApplicationState.GrsDataReceived,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Missing data from GRS, redirecting to start GRS registration")
          Redirect(AppRoutes.apply.AgentApplicationController.startRegistration)
    )
    .ensure(
      condition = _.agentApplication.refusalToDealWithCheck.forall(_ === EntityCheckResult.Fail),
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Refusal to deal with verification already done and passed. Redirecting to next check.")
          Redirect(nextPage)
    )
    .async:
      implicit request =>
        for
          checkResult <- agentAssuranceConnector
            .isRefusedToDealWith(request.agentApplication.getUtr)
          _ <- agentApplicationService
            .upsert(request.agentApplication
              .modify(_.refusalToDealWithCheck)
              .setTo(Some(checkResult)))
        yield checkResult match
          case EntityCheckResult.Pass => Redirect(nextPage)
          case EntityCheckResult.Fail => Redirect(failedCheckPage)

  private def failedCheckPage = AppRoutes.apply.entitycheckfailed.CanNotRegisterController.show
  private def nextPage(implicit request: AgentApplicationRequest[AnyContent]) =
    if (request.agentApplication.businessType === BusinessType.SoleTrader)
      AppRoutes.apply.internal.RefusalToDealWithController.check()
    else
      AppRoutes.apply.internal.CompaniesHouseStatusController.check()
