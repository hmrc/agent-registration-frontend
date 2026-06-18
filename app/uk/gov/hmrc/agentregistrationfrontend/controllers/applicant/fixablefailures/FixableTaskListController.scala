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
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication.Outcome
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.fixableTaskListStatus
import uk.gov.hmrc.agentregistrationfrontend.model.isSoleTraderOwner
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.DisplayDate.displayDateForLang
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.FixableTaskListPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FixableTaskListController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  taskListPage: FixableTaskListPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] =
    actions
      .getApplicationAfterSentForRisking
      .refine:
        implicit request =>
          request.get[AgentApplication].riskingOutcomeApplication match
            case Some(overallOutcome) if overallOutcome.outcome === Outcome.FailedFixable => request.add[RiskingOutcomeApplication](overallOutcome)
            case _ =>
              logger.warn("Risking outcome is not fixable. Redirecting to where outcome can be handled.")
              Redirect(AppRoutes.apply.AgentApplicationController.applicationStatus)
      .refine(implicit request =>
        val agentApplication: AgentApplication = request.get
        individualProvideDetailsService.findAllByApplicationId(agentApplication.agentApplicationId).map: individualsList =>
          request.add[List[IndividualProvidedDetails]](individualsList)
      ):
        implicit request =>
          val agentApplication: AgentApplication = request.get
          val overallOutcome: RiskingOutcomeApplication = request.get
          val riskingOutcomeEntity: RiskingOutcomeEntity = agentApplication.getRiskingOutcomeEntity
          val allIndividuals: List[IndividualProvidedDetails] = request.get
          Ok(taskListPage(
            taskListStatus = agentApplication.fixableTaskListStatus(
              riskingOutcomeEntity = riskingOutcomeEntity,
              fixableIndividuals = allIndividuals
                .filter(!_.providedByApplicant.contains(true))
                .map(_.getRiskingOutcomeIndividual)
                .collect:
                  case fixable: RiskingOutcomeIndividual.FailedFixable => fixable
            ),
            entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
            correctiveActionExpiryDate = displayDateForLang(overallOutcome.correctiveActionExpiryDate),
            isSoleTrader = agentApplication.isSoleTraderOwner
          ))
