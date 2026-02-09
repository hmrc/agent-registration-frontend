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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.individual.UserProvidedSaUtr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions

import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualSaUtrForm
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.IndividualSaUtrPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndividualSaUtrController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  view: IndividualSaUtrPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[DataWithIndividualProvidedDetails] = actions.getProvideDetailsInProgress
    .ensure(
      _.individualProvidedDetails.individualNino.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualNinoController.show.url)
    )
    .ensure(
      _.individualProvidedDetails.individualSaUtr.fold(true) {
        case IndividualSaUtr.FromAuth(_) | IndividualSaUtr.FromCitizenDetails(_) => false
        case _ => true
      },
      implicit request =>
        logger.info(s"SaUtr is already provided from auth or citizen details. Skipping page and moving to next page.")
        Redirect(AppRoutes.providedetails.CheckYourAnswersController.show.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        IndividualSaUtrForm.form
          .fill:
            request
              .individualProvidedDetails
              .individualSaUtr
              .map(_.toUserProvidedSaUtr)
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[UserProvidedSaUtr](
        IndividualSaUtrForm.form,
        implicit r => view(_)
      )
      .async:
        implicit request =>
          val validFormData: UserProvidedSaUtr = request.get
          val updatedApplication: IndividualProvidedDetailsToBeDeleted = request
            .individualProvidedDetails
            .modify(_.individualSaUtr)
            .setTo(Some(validFormData))
          individualProvideDetailsService
            .upsert(updatedApplication)
            .map: _ =>
              Redirect(AppRoutes.providedetails.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater
