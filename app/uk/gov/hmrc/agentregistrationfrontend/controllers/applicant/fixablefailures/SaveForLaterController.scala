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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentRegistrationRiskingService
import uk.gov.hmrc.agentregistrationfrontend.util.DisplayDate.displayDateForLang
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.SaveForLaterPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveForLaterController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  saveForLaterPage: SaveForLaterPage,
  agentRegistrationRiskingService: AgentRegistrationRiskingService
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] =
    actions
      .getApplicationAfterSentForRisking
      .refine(implicit request =>
        agentRegistrationRiskingService
          .getRiskingProgress(request.agentApplication.applicationReference)
          .map: riskingProgress =>
            request.add[RiskingProgress](riskingProgress)
      )
      .refine(implicit request =>
        request.get[RiskingProgress] match
          case failedFixable: RiskingProgress.FailedFixable => request.replace[RiskingProgress, RiskingProgress.FailedFixable](failedFixable)
          case _ => throw new IllegalStateException("Risking progress is not in a failed fixable state")
      ):
        implicit request =>
          val failedFixable: RiskingProgress.FailedFixable = request.get
          Ok(saveForLaterPage(
            correctiveActionExpiryDate = displayDateForLang(failedFixable.correctiveActionExpiryDate)
          ))
