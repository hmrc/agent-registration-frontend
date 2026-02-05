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
import uk.gov.hmrc.agentregistration.shared.llp.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistration.shared.llp.UserProvidedDateOfBirth
import uk.gov.hmrc.agentregistrationfrontend.action.IndividualActions

import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualDateOfBirthForm
import uk.gov.hmrc.agentregistrationfrontend.services.llp.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.individualconfirmation.IndividualDateOfBirthPage

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndividualDateOfBirthController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  view: IndividualDateOfBirthPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)(using clock: Clock)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[DataWithIndividualProvidedDetails] = actions.getProvideDetailsInProgress
    .ensure(
      _.individualProvidedDetails.emailAddress.isDefined,
      implicit request =>
        logger.info("Email address not yet provided. Redirecting to email page.")
        Redirect(AppRoutes.providedetails.IndividualEmailAddressController.show.url)
    )
    .ensure(
      _.individualProvidedDetails.individualDateOfBirth.fold(true) {
        case IndividualDateOfBirth.FromCitizensDetails(_) => false
        case _ => true
      },
      implicit request =>
        logger.info(s"Date of birth is already provided from citizens details. Skipping page and moving to next page.")
        Redirect(AppRoutes.providedetails.CheckYourAnswersController.show.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        IndividualDateOfBirthForm.form
          .fill:
            request
              .individualProvidedDetails
              .individualDateOfBirth
              .map(_.toUserProvidedDateOfBirth)
      ))

  def submit: Action[AnyContent] = baseAction
    .ensureValidForm[UserProvidedDateOfBirth](
      IndividualDateOfBirthForm.form,
      implicit r => view(_)
    )
    .async:
      implicit request =>
        val validFormData: UserProvidedDateOfBirth = request.get
        val updatedApplication: IndividualProvidedDetailsToBeDeleted = request
          .individualProvidedDetails
          .modify(_.individualDateOfBirth)
          .setTo(Some(validFormData))
        individualProvideDetailsService
          .upsert(updatedApplication)
          .map: _ =>
            Redirect(AppRoutes.providedetails.CheckYourAnswersController.show.url)
