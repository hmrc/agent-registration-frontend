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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.providedbyapplicant

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.SelectIndividualForm
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.repository.ProvidedByApplicantSessionStore
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.providedbyapplicant.SelectIndividualPage

@Singleton
class SelectIndividualController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: SelectIndividualPage,
  individualProvideDetailsService: IndividualProvideDetailsService,
  providedByApplicantSessionStore: ProvidedByApplicantSessionStore
)
extends FrontendController(mcc, actions):

  private type DataWithListOfIncompleteIndividuals = List[IndividualProvidedDetails] *: DataWithApplication

  private val baseAction: ActionBuilderWithData[DataWithListOfIncompleteIndividuals] = actions
    .getApplicationInProgress
    .ensure(
      condition =
        implicit request =>
          request.get[AgentApplication].getUserRole =!= UserRole.Owner,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Sole trader owners do not provide details like this, redirecting to task list for the correct links")
          Redirect(AppRoutes.apply.TaskListController.show.url)
    )
    .refine:
      implicit request =>
        val agentApplication: AgentApplication = request.get
        individualProvideDetailsService
          .findAllByApplicationId(agentApplication.agentApplicationId)
          .map: individualsList =>
            val incompleteIndividuals: List[IndividualProvidedDetails] = individualsList.filterNot(_.hasFinished)
            if incompleteIndividuals.isEmpty
            then
              logger.warn("There are no individuals with incomplete details, redirecting to progress page which will show status of all individuals")
              Redirect(AppRoutes.apply.listdetails.progress.CheckProgressController.show.url)
            else
              request.add[List[IndividualProvidedDetails]](incompleteIndividuals)

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val incompleteIndividuals: List[IndividualProvidedDetails] = request.get
      val form = SelectIndividualForm.form(incompleteIndividuals)
      Ok(view(
        form = form,
        incompleteIndividuals = incompleteIndividuals
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[IndividualProvidedDetails](
        form =
          (request: RequestWithData[DataWithListOfIncompleteIndividuals]) =>
            SelectIndividualForm.form(request.get[List[IndividualProvidedDetails]]),
        resultToServeWhenFormHasErrors =
          implicit request: RequestWithData[DataWithListOfIncompleteIndividuals] =>
            (formWithErrors: Form[IndividualProvidedDetails]) =>
              view(
                form = formWithErrors,
                incompleteIndividuals = request.get[List[IndividualProvidedDetails]]
              )
      )
      .async:
        implicit request: RequestWithData[IndividualProvidedDetails *: DataWithListOfIncompleteIndividuals] =>
          val i: IndividualProvidedDetails = request.get
          val providedByApplicant: ProvidedByApplicant = ProvidedByApplicant(
            individualProvidedDetailsId = i._id,
            individualName = i.individualName
          )
          providedByApplicantSessionStore
            .upsert(providedByApplicant)
            .map: _ =>
              Ok(providedByApplicant.toString) // TODO: Date of Birth should be next page even if coming from CYA as changing individual wipes everything - ideally via CYA controller when available
      .redirectIfSaveForLater
