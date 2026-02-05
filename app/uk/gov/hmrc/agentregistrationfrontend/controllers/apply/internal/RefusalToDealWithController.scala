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
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefusalToDealWithController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  agentAssuranceConnector: AgentAssuranceConnector,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  def check(): Action[AnyContent] = actions
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
      condition = _.agentApplication.isRefusalToDealWithCheckRequired,
      resultWhenConditionNotMet =
        implicit request =>
          logger.info("Refusal to deal with verification already done and passed. Redirecting to next check.")
          Redirect(nextCheckEndpoint)
    )
    .async:
      implicit request =>
        for
          checkResult <- agentAssuranceConnector
            .checkForRefusalToDealWith(request.agentApplication.getUtr)
          _ <- agentApplicationService
            .upsert(request.agentApplication
              .modify(_.refusalToDealWithCheckResult)
              .setTo(Some(checkResult)))
        yield checkResult match
          case CheckResult.Pass => Redirect(nextCheckEndpoint)
          case CheckResult.Fail => Redirect(failedCheckPage)

  private def failedCheckPage: Call = AppRoutes.apply.checkfailed.CanNotRegisterController.show
  private def nextCheckEndpoint: Call = AppRoutes.apply.internal.DeceasedController.check()

  extension (agentApplication: AgentApplication)
    private def isRefusalToDealWithCheckRequired: Boolean = agentApplication.refusalToDealWithCheckResult =!= Some(CheckResult.Pass)
