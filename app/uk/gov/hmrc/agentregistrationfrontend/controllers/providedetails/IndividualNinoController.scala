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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails

import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.llp.IndividualNino
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistration.shared.llp.UserProvidedNino
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions

import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualNinoForm
import uk.gov.hmrc.agentregistrationfrontend.services.llp.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.individualconfirmation.IndividualNinoPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndividualNinoController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  view: IndividualNinoPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[DataWithIndividualProvidedDetails] = actions.getProvideDetailsInProgress
    .ensure(
      _.individualProvidedDetails.emailAddress.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualEmailAddressController.show.url)
    )
    .ensure(
      _.individualProvidedDetails.individualNino.fold(true) {
        case IndividualNino.FromAuth(_) => false
        case _ => true
      },
      implicit request =>
        logger.info(s"Nino is already provided from auth or citizen details. Skipping page and moving to next page.")
        Redirect(AppRoutes.providedetails.CheckYourAnswersController.show.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        IndividualNinoForm.form
          .fill:
            request
              .individualProvidedDetails
              .individualNino
              .map(_.toUserProvidedNino)
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[UserProvidedNino](
        IndividualNinoForm.form,
        implicit r => view(_)
      )
      .async:
        implicit request =>
          val validFormData: UserProvidedNino = request.get
          val updatedApplication: IndividualProvidedDetailsToBeDeleted = request
            .individualProvidedDetails
            .modify(_.individualNino)
            .setTo(Some(validFormData))
          individualProvideDetailsService
            .upsert(updatedApplication)
            .map: _ =>
              Redirect(AppRoutes.providedetails.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater

  extension (individualNino: IndividualNino)
    def toUserProvidedNino: UserProvidedNino =
      individualNino match
        case u: UserProvidedNino => u
        case h: IndividualNino.FromAuth => throw new IllegalArgumentException(s"Nino is already provided from auth enrolments (${h.nino})")
