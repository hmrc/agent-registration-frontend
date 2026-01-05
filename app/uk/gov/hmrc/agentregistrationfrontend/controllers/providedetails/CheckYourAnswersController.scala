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
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.MemberProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.memberconfirmation.CheckYourAnswersPage

@Singleton
class CheckYourAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: CheckYourAnswersPage
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[MemberProvideDetailsRequest, AnyContent] = actions
    .Member
    .getProvideDetailsInProgress
    .ensure(
      _.memberProvidedDetails.companiesHouseMatch.flatMap(_.companiesHouseOfficer).isDefined,
      implicit request =>
        Redirect(AppRoutes.providedetails.CompaniesHouseNameQueryController.show)
    )
    .ensure(
      _.memberProvidedDetails.telephoneNumber.isDefined,
      implicit request =>
        Redirect(AppRoutes.providedetails.MemberTelephoneNumberController.show)
    )
    .ensure(
      _.memberProvidedDetails.emailAddress.exists(_.isVerified),
      implicit request =>
        Redirect(AppRoutes.providedetails.MemberEmailAddressController.show)
    )
    .ensure(
      _.memberProvidedDetails.memberNino.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.MemberNinoController.show)
    )
    .ensure(
      _.memberProvidedDetails.memberSaUtr.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.MemberSaUtrController.show)
    )
    .ensure(
      _.memberProvidedDetails.hasApprovedApplication.getOrElse(false),
      implicit request =>
        Redirect(AppRoutes.providedetails.MemberApproveApplicantController.show)
    )
    .ensure(
      _.memberProvidedDetails.hmrcStandardForAgentsAgreed === StateOfAgreement.Agreed,
      implicit request =>
        Redirect(AppRoutes.providedetails.MemberHmrcStandardForAgentsController.show.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request => Ok(view())
