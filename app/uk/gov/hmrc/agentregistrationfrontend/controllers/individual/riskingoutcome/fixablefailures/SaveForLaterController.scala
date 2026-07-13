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
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.util.DisplayDate.displayDateForLang
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingoutcome.fixablefailures.SaveForLaterPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveForLaterController @Inject() (
  mcc: MessagesControllerComponents,
  actions: IndividualActions,
  saveForLaterPage: SaveForLaterPage,
  appConfig: AppConfig
)
extends FrontendController(mcc, actions):

  def show(linkId: LinkId): Action[AnyContent] =
    actions
      .authorisedWithFailedFixable(linkId)
      .behindFeatureFlag(appConfig.Features.fixableFailures)
      .refine(implicit request =>
        request.get[AgentApplication].riskingOutcomeApplication match
          case Some(failedFixable: RiskingOutcomeApplication.FailedFixable) if failedFixable.reSubmittedAt.isEmpty =>
            request.add[RiskingOutcomeApplication.FailedFixable](failedFixable)
          case _ => Redirect(AppRoutes.providedetails.riskingoutcome.RiskingOutcomeController.show(linkId))
      ):
        implicit request =>
          val failedFixable: RiskingOutcomeApplication.FailedFixable = request.get
          Ok(saveForLaterPage(
            correctiveActionExpiryDate = displayDateForLang(failedFixable.correctiveActionExpiryDate),
            linkId = linkId
          ))
