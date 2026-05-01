/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.individual.UserProvidedDateOfBirth
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.ApplicantProvidedDoBForm
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.repository.ProvidedByApplicantSessionStore
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.providedbyapplicant.ApplicantProvidedDoBPage

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicantProvidedDoBController @Inject() (
  actions: ApplicantActions,
  mcc: MessagesControllerComponents,
  providedByApplicantSessionStore: ProvidedByApplicantSessionStore,
  view: ApplicantProvidedDoBPage
)(using clock: Clock)
extends FrontendController(mcc, actions):

  private type AgentProvidedIndividualDetails = ProvidedByApplicant *: AgentApplication *: DataWithAuth

  private def baseAction: ActionBuilderWithData[AgentProvidedIndividualDetails] = actions
    .getApplicationInProgress
    .ensure(
      condition = implicit request => request.get[AgentApplication].getUserRole =!= UserRole.Owner,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Sole trader attempting to access applicant provided date of birth page")
          Redirect(AppRoutes.apply.TaskListController.show.url)
    )
    .refine:
      implicit request =>
        providedByApplicantSessionStore.find().map:
          case Some(details) => request.add[ProvidedByApplicant](details)
          case _ => Redirect(AppRoutes.apply.listdetails.providedbyapplicant.SelectIndividualController.show.url)

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(
        view(
          form = ApplicantProvidedDoBForm.form,
          applicantProvidedName = Some(request.get[ProvidedByApplicant].individualName.value)
        )
      )

  def submit: Action[AnyContent] = baseAction
    .ensureValidForm[UserProvidedDateOfBirth](
      form = ApplicantProvidedDoBForm.form,
      resultToServeWhenFormHasErrors =
        implicit request =>
          formWithErrors =>
            BadRequest(
              view(
                form = formWithErrors,
                applicantProvidedName = Some(request.get[ProvidedByApplicant].individualName.value)
              )
            )
    )
    .async:
      implicit request =>
        val applicantProvidedDetails: ProvidedByApplicant = request.get
        val date: UserProvidedDateOfBirth = request.get
        val updatedDetails: ProvidedByApplicant = ProvidedByApplicant(
          individualProvidedDetailsId = applicantProvidedDetails.individualProvidedDetailsId,
          individualName = applicantProvidedDetails.individualName,
          individualDateOfBirth = Some(date)
        )
        providedByApplicantSessionStore
          .upsert(updatedDetails)
          .map: _ =>
            Redirect(AppRoutes.providedetails.ExitController.genericExitPage)
