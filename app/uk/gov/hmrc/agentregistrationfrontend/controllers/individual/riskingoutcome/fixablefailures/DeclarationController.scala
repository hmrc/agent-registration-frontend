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

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingoutcome.fixablefailures.DeclarationPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeclarationController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  view: DeclarationPage
)
extends FrontendController(mcc, actions):

  private def baseAction(
    linkId: LinkId
  ): ActionBuilderWithData[DataWithRiskingOutcome] = authorisedWithRiskingOutcome(linkId)
    .ensure(
      condition =
        implicit request =>
          val individualRiskingOutcome: RiskingOutcomeIndividual = request.get
          individualRiskingOutcome match
            case outcome @ RiskingOutcomeIndividual.FailedFixable(fixes: Seq[IndividualFix]) => fixes.forall(_.isConfirmed.contains(true))
            case _ => false,
      resultWhenConditionNotMet =
        implicit r =>
          Redirect(AppRoutes.providedetails.riskingoutcome.RiskingOutcomeController.show(linkId))
    )

  def show(linkId: LinkId): Action[AnyContent] =
    baseAction(linkId):
      implicit request =>
        Ok(view(
          linkId = linkId
        ))

  def submit(linkId: LinkId): Action[AnyContent] =
    baseAction(linkId):
      implicit request =>
        Redirect(AppRoutes.providedetails.riskingoutcome.fixablefailures.IndividualConfirmationController.show(linkId))
