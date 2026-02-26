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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant

import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.taskListStatus
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentRegistrationRiskingService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.DeclarationPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeclarationController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: DeclarationPage,
  agentApplicationService: AgentApplicationService,
  individualProvideDetailsService: IndividualProvideDetailsService,
  agentRegistrationRiskingService: AgentRegistrationRiskingService
)
extends FrontendController(mcc, actions):

  private type DataWithIndividuals = List[IndividualProvidedDetails] *: AgentApplication *: DataWithAuth

  private val baseAction: ActionBuilderWithData[DataWithIndividuals] = actions
    .getApplicationInProgress
    .refine:
      implicit request =>
        val agentApplication: AgentApplication = request.get
        individualProvideDetailsService
          .findAllByApplicationId(agentApplication.agentApplicationId)
          .map: individualsList =>
            request.add[List[IndividualProvidedDetails]](individualsList)
    .ensure(
      condition =
        implicit request =>
          request.agentApplication
            .taskListStatus(request.get[List[IndividualProvidedDetails]])
            .declaration
            .canStart,
      implicit request =>
        logger.warn("Cannot start declaration whilst tasks are outstanding, redirecting to task list")
        Redirect(AppRoutes.apply.TaskListController.show)
    )

  def show: Action[AnyContent] = baseAction
    .getBusinessPartnerRecord:
      implicit request =>
        Ok(view(
          entityName = request.businessPartnerRecordResponse.getEntityName,
          agentApplication = request.agentApplication
        ))

  def submit: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        for
          _ <- agentRegistrationRiskingService.submitForRisking(
            SubmitForRiskingRequest(
              agentApplication = request.agentApplication,
              individuals = request.get[List[IndividualProvidedDetails]]
            )
          )
          _ <- agentApplicationService
            .upsert(
              request.agentApplication
                .modify(_.applicationState)
                .setTo(ApplicationState.SentForRisking)
            )
        yield Redirect(AppRoutes.apply.AgentApplicationController.applicationSubmitted)
