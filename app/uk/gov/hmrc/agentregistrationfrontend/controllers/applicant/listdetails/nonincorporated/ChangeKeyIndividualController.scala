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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.nonincorporated

import com.softwaremill.quicklens.*
import play.api.data.Form
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChangeKeyIndividualController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  enterIndividualNamePageRenderer: EnterIndividualNamePageRenderer,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private type DataWithIndividual = IndividualProvidedDetails *: DataWithUnincorporatedPartnershipKeyIndividuals

  private def baseAction(individualProvidedDetailsId: IndividualProvidedDetailsId): ActionBuilderWithData[DataWithIndividual] = actions
    .getKeyIndividualsForUnincorporatedPartnership
    .refine:
      implicit request =>
        request.get[List[IndividualProvidedDetails]].find(_._id === individualProvidedDetailsId) match
          case Some(individualProvidedDetails) => request.add[IndividualProvidedDetails](individualProvidedDetails)
          case None =>
            logger.warn(
              s"Individual provided details record not found for the provided id ${individualProvidedDetailsId.value} - redirecting as this may be due to a stale link or an attempt to access another record that does not belong to the current application"
            )
            Redirect(AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show.url)

  def show(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = baseAction(individualProvidedDetailsId)
    .async:
      implicit request =>
        enterIndividualNamePageRenderer
          .render(
            form = IndividualNameForm.form.fill(request.get[IndividualProvidedDetails].individualName),
            formAction = formAction(individualProvidedDetailsId)
          )(using request.delete[IndividualProvidedDetails])
          .map(Ok(_))

  def submit(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] =
    baseAction(individualProvidedDetailsId)
      .ensureValidFormAndRedirectIfSaveForLater[IndividualName](
        form = IndividualNameForm.form,
        resultToServeWhenFormHasErrors =
          implicit request =>
            (formWithErrors: Form[IndividualName]) =>
              enterIndividualNamePageRenderer.render(
                form = formWithErrors,
                formAction = formAction(individualProvidedDetailsId)
              )(using request.delete[IndividualProvidedDetails])
      )
      .async:
        implicit request =>
          val individualNameFromForm: IndividualName = request.get
          val individualToChange: IndividualProvidedDetails = request.get
          individualProvideDetailsService
            .upsertForApplication(
              individualToChange
                .modify(_.individualName)
                .setTo(individualNameFromForm)
            )
            .map: _ =>
              Redirect(AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show)
      .redirectIfSaveForLater

  private def formAction(individualProvidedDetailsId: IndividualProvidedDetailsId): Call = AppRoutes
    .apply
    .listdetails
    .nonincorporated
    .ChangeKeyIndividualController
    .submit(individualProvidedDetailsId)
