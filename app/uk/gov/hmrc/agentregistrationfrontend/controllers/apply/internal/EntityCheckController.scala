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
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.EntityCheckService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntityCheckController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  entityCheckService: EntityCheckService,
  agentApplicationService: AgentApplicationService,
  simplePage: SimplePage
)
extends FrontendController(mcc, actions):

  val baseAction = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      condition =
        _.agentApplication
          .asLlpApplication
          .businessDetails
          .isDefined,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Missing data from GRS, redirecting to start GRS registration")
          Redirect(AppRoutes.apply.AgentApplicationController.startRegistration)
    )
    .ensure(
      condition = _.agentApplication.hmrcEntityVerificationPassed.isEmpty,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Entity verification already done. Redirecting to task list page.")
          Redirect(AppRoutes.apply.TaskListController.show)
    )

  def verifyEntity(): Action[AnyContent] = baseAction
    .async:
      implicit request =>
        val llpApplication = request.agentApplication.asLlpApplication

        for
          checkResult <- entityCheckService
            .refusalToDealCheck(llpApplication.getBusinessDetails.saUtr)
          _ <- agentApplicationService
            .upsert(llpApplication
              .modify(_.hmrcEntityVerificationPassed)
              .setTo(Some(checkResult)))
        yield checkResult match
          case EntityCheckResult.Pass => Redirect(AppRoutes.apply.TaskListController.show)
          case EntityCheckResult.Fail =>
            logger.warn("Entity verification failed. Redirecting to verification failed page.")
            Ok(simplePage(
              h1 = "Entity verification failed...",
              bodyText = Some("Placeholder for entity verification failed page...")
            ))
