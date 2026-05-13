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
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistration.shared.individual.UserProvidedNino
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.providedbyapplicant.ApplicantProvidedNinoPage
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.providedbyapplicant.ApplicantProvidedNinoForm
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.repository.ProvidedByApplicantSessionStore
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import com.softwaremill.quicklens.modify

import javax.inject.Inject

class ApplicantProvidedNinoController @Inject() (
  actions: ApplicantActions,
  mcc: MessagesControllerComponents,
  providedByApplicantSessionStore: ProvidedByApplicantSessionStore,
  view: ApplicantProvidedNinoPage
)
extends FrontendController(mcc, actions):

  private type AgentProvidedIndividualDetails = ProvidedByApplicant *: AgentApplication *: DataWithAuth

  private def baseAction: ActionBuilderWithData[AgentProvidedIndividualDetails] = actions
    .getApplicationInProgress
    .ensure(
      condition = implicit request => request.get[AgentApplication].getUserRole =!= UserRole.Owner,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Sole trader attempting to access applicant provided nino page")
          Redirect(AppRoutes.apply.TaskListController.show.url)
    )
    .refine:
      implicit request =>
        providedByApplicantSessionStore
          .find()
          .map:
            case Some(providedByApplicant) => request.add[ProvidedByApplicant](providedByApplicant)
            case None =>
              logger.warn("No ProvidedByApplicant details found in session when trying to access Nino page, redirecting to select individual page")
              Redirect(AppRoutes.apply.listdetails.providedbyapplicant.SelectIndividualController.show.url)

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        form = ApplicantProvidedNinoForm.form
          .fill:
            request
              .get[ProvidedByApplicant]
              .individualNino
              .map(_.toUserProvidedNino)
        ,
        applicantProvidedName = Some(request.get[ProvidedByApplicant].individualName.value)
      ))

  def submit: Action[AnyContent] = baseAction.ensureValidForm[UserProvidedNino](
    form = ApplicantProvidedNinoForm.form,
    resultToServeWhenFormHasErrors =
      implicit request =>
        formWithErrors =>
          BadRequest(
            view(
              form = formWithErrors,
              applicantProvidedName = Some(request.get[ProvidedByApplicant].individualName.value)
            )
          )
  ).async:
    implicit request =>
      val nino: UserProvidedNino = request.get
      val providedByApplicant: ProvidedByApplicant = request.get
      val updatedProvidedDetails: ProvidedByApplicant = providedByApplicant
        .modify(_.individualNino)
        .setTo(Some(nino))
      providedByApplicantSessionStore
        .upsert(updatedProvidedDetails)
        .map: _ =>
          Redirect(AppRoutes.apply.listdetails.providedbyapplicant.CheckYourAnswersController.show.url)

  extension (individualNino: IndividualNino)
    def toUserProvidedNino: UserProvidedNino =
      individualNino match
        case u: UserProvidedNino => u
        case h: IndividualNino.FromAuth => throw new IllegalArgumentException(s"Nino is already provided from auth enrolments (${h.nino})")
