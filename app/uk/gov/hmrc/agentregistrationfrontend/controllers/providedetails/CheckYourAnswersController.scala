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

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistration.shared.llp.ProvidedDetailsState.Finished
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.action.individual.llp.IndividualProvideDetailsRequest

import uk.gov.hmrc.agentregistrationfrontend.services.llp.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.individualconfirmation.CheckYourAnswersPage

@Singleton
class CheckYourAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: IndividualActions,
  view: CheckYourAnswersPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[IndividualProvideDetailsRequest, AnyContent] = actions
    .getProvideDetailsInProgress
    .ensure(
      _.individualProvidedDetails.companiesHouseMatch.flatMap(_.companiesHouseOfficer).isDefined,
      implicit request =>
        Redirect(AppRoutes.providedetails.CompaniesHouseNameQueryController.show)
    )
    .ensure(
      _.individualProvidedDetails.telephoneNumber.isDefined,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualTelephoneNumberController.show)
    )
    .ensure(
      _.individualProvidedDetails.emailAddress.exists(_.isVerified),
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualEmailAddressController.show)
    )
    .ensure(
      _.individualProvidedDetails.individualDateOfBirth.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualDateOfBirthController.show)
    )
    .ensure(
      _.individualProvidedDetails.individualNino.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualNinoController.show)
    )
    .ensure(
      _.individualProvidedDetails.individualSaUtr.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualSaUtrController.show)
    )
    .ensure(
      _.individualProvidedDetails.hasApprovedApplication.getOrElse(false),
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualApproveApplicantController.show)
    )
    .ensure(
      _.individualProvidedDetails.hmrcStandardForAgentsAgreed === StateOfAgreement.Agreed,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualHmrcStandardForAgentsController.show.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request => Ok(view(request.individualProvidedDetails))

  def submit: Action[AnyContent] = baseAction.async:
    implicit request =>
      individualProvideDetailsService
        .upsert(
          request.individualProvidedDetails
            .modify(_.providedDetailsState)
            .setTo(Finished)
        ).map: _ =>
          Redirect(AppRoutes.providedetails.IndividualConfirmationController.show)
