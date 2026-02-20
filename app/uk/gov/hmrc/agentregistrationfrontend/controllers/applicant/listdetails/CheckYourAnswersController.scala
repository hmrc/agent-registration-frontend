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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsNotSoleTrader
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfRequiredKeyIndividuals
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.CheckYourAnswersPage

@Singleton
class CheckYourAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: CheckYourAnswersPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private type DataWithLists = List[IndividualProvidedDetails] *: IsNotSoleTrader *: DataWithAuth

  private val baseAction: ActionBuilderWithData[DataWithLists] = actions
    .getApplicationInProgress
    .refine:
      implicit request =>
        request.get[AgentApplication] match
          case _: AgentApplicationSoleTrader =>
            logger.warn("Sole traders do not add other relevant individuals to a list, redirecting to task list for the correct links")
            Redirect(AppRoutes.apply.TaskListController.show.url)
          case aa: IsNotSoleTrader => request.replace[AgentApplication, IsNotSoleTrader](aa)
    .refine:
      implicit request =>
        val agentApplication: IsNotSoleTrader = request.get[IsNotSoleTrader]
        individualProvideDetailsService
          .findAllByApplicationId(agentApplication.agentApplicationId)
          .map: individualsList =>
            request.add[List[IndividualProvidedDetails]](individualsList)
    .ensure(
      condition =
        implicit request =>
          request.get[IsNotSoleTrader] match
            case a: AgentApplication.IsAgentApplicationForDeclaringNumberOfKeyIndividuals =>
              val partnersSize = request.get[List[IndividualProvidedDetails]].count(_.isPersonOfControl)
              NumberOfRequiredKeyIndividuals.isKeyIndividualListComplete(partnersSize, a.numberOfRequiredKeyIndividuals)
            case _ => true,
      resultWhenConditionNotMet =
        implicit request =>
          Redirect(AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show)
    )
    .ensure(
      condition = _.get[IsNotSoleTrader].hasOtherRelevantIndividuals.isDefined,
      resultWhenConditionNotMet =
        implicit request =>
          Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val allIndividualsList: List[IndividualProvidedDetails] = request.get
      val partnersList = allIndividualsList.filter(_.isPersonOfControl)
      val otherRelevantIndividualsList = allIndividualsList.filterNot(_.isPersonOfControl)
      val agentApplication = request.get[IsNotSoleTrader]

      Ok(view(
        agentApplication,
        partnersList,
        otherRelevantIndividualsList
      ))
