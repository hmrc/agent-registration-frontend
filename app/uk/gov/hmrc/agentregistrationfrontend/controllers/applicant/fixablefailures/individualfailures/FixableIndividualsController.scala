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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures.individualfailures

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication.Outcome
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.DisplayDate.displayDateForLang
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.individualfailures.FixableIndividualsPage

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FixableIndividualsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  individualProvideDetailsService: IndividualProvideDetailsService,
  fixableIndividualsPage: FixableIndividualsPage
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] =
    actions
      .getApplicationAfterSentForRisking
      .ensure(
        condition =
          implicit request =>
            request
              .get[AgentApplication]
              .riskingOutcomeApplication
              .exists(_.outcome === Outcome.FailedFixable),
        resultWhenConditionNotMet =
          implicit request =>
            logger.warn("Risking outcome is not fixable. Redirecting to where outcome can be handled.")
            Redirect(AppRoutes.apply.AgentApplicationController.applicationStatus)
      )
      .refine(implicit request =>
        val agentApplication: AgentApplication = request.get
        individualProvideDetailsService.findAllByApplicationId(agentApplication.agentApplicationId).map: individualsList =>
          val fixable = individualsList.filter(_.isFixable)
          if fixable.nonEmpty
          then request.add[List[IndividualProvidedDetails]](fixable)
          else
            logger.warn("Individuals risking outcomes are not fixable. Redirecting to where outcome can be handled.")
            Redirect(AppRoutes.apply.AgentApplicationController.applicationStatus)
      ):
        implicit request =>
          // TODO: replace with deadline date
          val deadlineDate = LocalDate.MAX
          Ok(fixableIndividualsPage(
            dateOfDeadline = displayDateForLang(Some(deadlineDate)),
            fixableIndividualsList = request.get[List[IndividualProvidedDetails]],
            agentApplication = request.get[AgentApplication]
          ))

  extension (ipd: IndividualProvidedDetails)
    private def isFixable: Boolean = ipd.riskingOutcomeIndividual.contains(RiskingOutcomeIndividual.FailedFixable(_))
