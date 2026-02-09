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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.link

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.link.LinkPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: LinkPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private type DataWithList = List[IndividualProvidedDetails] *: DataWithApplication

  private val baseAction: ActionBuilderWithData[DataWithList] = actions
    .getApplicationInProgress
    .refine(implicit request =>
      val agentApplication: AgentApplication = request.get
      individualProvideDetailsService.findAllByApplicationId(agentApplication.agentApplicationId).map: individualsList =>
        request.add[List[IndividualProvidedDetails]](individualsList)
    )
    .ensure(
      condition =
        implicit request =>
          request.get[List[IndividualProvidedDetails]].nonEmpty,
      resultWhenConditionNotMet =
        implicit request =>
          Redirect(AppRoutes.apply.TaskListController.show)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        agentApplication = request.get[AgentApplication],
        existingList = request.get[List[IndividualProvidedDetails]]
      ))

  def submit: Action[AnyContent] = baseAction
    .async:
      // update each individual provided details to mark them as having been sent the link
      implicit request =>
        val individualsList: List[IndividualProvidedDetails] = request.get[List[IndividualProvidedDetails]]
        individualProvideDetailsService
          .markLinkSent(individualsList)
          .map: _ =>
            Redirect(AppRoutes.apply.TaskListController.show)
