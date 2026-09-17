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
import uk.gov.hmrc.agentregistrationfrontend.util.UniqueTuple.PresentIn
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

  def show(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = actions
    .getKeyIndividualsForUnincorporatedPartnership
    .async:
      implicit request =>
        enterIndividualNamePageRenderer
          .render(
            form = IndividualNameForm.form.fill(keyIndividual(individualProvidedDetailsId).individualName),
            formAction = formAction(individualProvidedDetailsId)
          )
          .map(Ok(_))

  def submit(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] =
    actions
      .getKeyIndividualsForUnincorporatedPartnership
      .ensureValidFormAndRedirectIfSaveForLater[IndividualName](
        form = IndividualNameForm.form,
        resultToServeWhenFormHasErrors =
          implicit request =>
            (formWithErrors: Form[IndividualName]) =>
              enterIndividualNamePageRenderer.render(
                form = formWithErrors,
                formAction = formAction(individualProvidedDetailsId)
              )
      )
      .async:
        implicit request =>
          val individualNameFromForm: IndividualName = request.get
          individualProvideDetailsService
            .upsertForApplication(
              keyIndividual(individualProvidedDetailsId)
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

  /** The key individual being changed, from the list the action already loaded. */
  private inline def keyIndividual[Data <: Tuple](
    individualProvidedDetailsId: IndividualProvidedDetailsId
  )(using
    request: RequestWithData[Data],
    ev: List[IndividualProvidedDetails] PresentIn Data
  ): IndividualProvidedDetails = request
    .get[List[IndividualProvidedDetails]]
    .find(_._id === individualProvidedDetailsId)
    .getOrThrowExpectedDataMissing(
      s"IndividualProvidedDetails with id $individualProvidedDetailsId not found"
    )
