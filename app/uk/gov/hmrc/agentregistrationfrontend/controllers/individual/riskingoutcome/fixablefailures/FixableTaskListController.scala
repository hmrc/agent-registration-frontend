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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual.riskingoutcome.fixablefailures

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.FixableIndividualTaskListStatus
import uk.gov.hmrc.agentregistrationfrontend.model.TaskStatus
import uk.gov.hmrc.agentregistrationfrontend.util.DisplayDate.displayDateForLang
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingoutcome.fixablefailures.FixableTaskListPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FixableTaskListController @Inject() (
  mcc: MessagesControllerComponents,
  actions: IndividualActions,
  taskListPage: FixableTaskListPage,
  appConfig: AppConfig
)
extends FrontendController(mcc, actions):

  def show(linkId: LinkId): Action[AnyContent] =
    actions
      .authorisedWithRiskingOutcome(linkId)
      .behindFeatureFlag(appConfig.Features.fixableFailures):
        implicit request =>
          val overallOutcome: RiskingOutcomeApplication = request.get
          val riskingOutcomeIndividual: RiskingOutcomeIndividual = request.get
          Ok(taskListPage(
            taskListStatus = fixableIndividualTaskListStatus(
              riskingOutcomeIndividual = riskingOutcomeIndividual
            ),
            correctiveActionExpiryDate = displayDateForLang(overallOutcome.correctiveActionExpiryDate),
            linkId = linkId
          ))

  private def fixableIndividualTaskListStatus(
    riskingOutcomeIndividual: RiskingOutcomeIndividual
  ): FixableIndividualTaskListStatus =
    val fixes: Seq[IndividualFix] =
      riskingOutcomeIndividual match
        case f: RiskingOutcomeIndividual.FailedFixable => f.fixes
        case _ => Seq.empty
    val fixesComplete: Boolean = fixes.forall(_.isConfirmed.contains(true)) | fixes.isEmpty
    val fixableTasks: Map[String, TaskStatus] =
      fixes.map(fix =>
        fix.toString -> TaskStatus(
          canStart = true,
          isComplete = fix.isConfirmed.contains(true)
        )
      ).toMap

    FixableIndividualTaskListStatus(
      fixableTasks = fixableTasks,
      declaration = TaskStatus(
        canStart = fixesComplete, // Declaration can be started only when all prior tasks are complete
        isComplete = false // Declaration is never "complete" until submission
      )
    )
