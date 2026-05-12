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

import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.individual.UserProvidedSaUtr
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.providedbyapplicant.SaUtrForm
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.repository.ProvidedByApplicantSessionStore
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.providedbyapplicant.SaUtrPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaUtrController @Inject() (
  actions: ApplicantActions,
  mcc: MessagesControllerComponents,
  view: SaUtrPage,
  providedByApplicantSessionStore: ProvidedByApplicantSessionStore
)
extends FrontendController(mcc, actions):

  private type AgentProvidedIndividualDetails = ProvidedByApplicant *: AgentApplication *: DataWithAuth

  private def baseAction: ActionBuilderWithData[AgentProvidedIndividualDetails] = actions
    .getApplicationInProgress
    .ensure(
      condition = implicit request => request.get[AgentApplication].getUserRole =!= UserRole.Owner,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Sole trader attempting to access applicant provided SaUtr page")
          Redirect(AppRoutes.apply.TaskListController.show.url)
    )
    .refine:
      implicit request =>
        providedByApplicantSessionStore
          .find()
          .map:
            case Some(providedByApplicant) => request.add[ProvidedByApplicant](providedByApplicant)
            case None =>
              logger.warn("No ProvidedByApplicant details found in session when trying to access SaUtr page, redirecting to select individual page")
              Redirect(AppRoutes.apply.listdetails.providedbyapplicant.SelectIndividualController.show.url)

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val providedByApplicant: ProvidedByApplicant = request.get
      Ok(view(
        form = SaUtrForm.form
          .fill:
            providedByApplicant
              .individualSaUtr
              .map(_.toUserProvidedSaUtr)
        ,
        individualName = providedByApplicant.individualName
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[UserProvidedSaUtr](
        SaUtrForm.form,
        implicit r => view(_, r.get[ProvidedByApplicant].individualName)
      )
      .async:
        implicit request =>
          val validFormData: UserProvidedSaUtr = request.get
          val updatedIndividualProvidedDetails: ProvidedByApplicant = request.get[ProvidedByApplicant]
            .modify(_.individualSaUtr)
            .setTo(Some(validFormData))
          providedByApplicantSessionStore
            .upsert(updatedIndividualProvidedDetails)
            .map: _ =>
              Redirect(AppRoutes.apply.listdetails.providedbyapplicant.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater
