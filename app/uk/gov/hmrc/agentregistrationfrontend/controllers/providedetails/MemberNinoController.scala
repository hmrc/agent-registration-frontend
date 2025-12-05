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
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.llp.MemberNino
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistration.shared.llp.UserProvidedNino
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.MemberProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.MemberNinoForm
import uk.gov.hmrc.agentregistrationfrontend.services.llp.MemberProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.memberconfirmation.MemberNinoPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberNinoController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  view: MemberNinoPage,
  memberProvideDetailsService: MemberProvideDetailsService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[MemberProvideDetailsRequest, AnyContent] = actions.Member.getProvideDetailsInProgress
    /*    .ensure(
      _.memberProvidedDetails.emailAddress.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.MemberEmailAddressController.show.url)
    )*/
    .ensure(
      _.memberProvidedDetails.memberNino.fold(true) {
        case MemberNino.FromAuth(_) => false
        case _ => true
      },
      implicit request =>
        logger.info(s"Nino is already provided from auth or citizen details. Skipping page and moving to next page.")
        Redirect(AppRoutes.providedetails.MemberSaUtrController.show.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        MemberNinoForm.form
          .fill:
            request
              .memberProvidedDetails
              .memberNino
              .map(_.toUserProvidedNino)
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[UserProvidedNino](
        MemberNinoForm.form,
        implicit r => view(_)
      )
      .async:
        implicit request: (MemberProvideDetailsRequest[AnyContent] & FormValue[UserProvidedNino]) =>
          val validFormData: MemberNino = request.formValue
          val updatedApplication: MemberProvidedDetails = request
            .memberProvidedDetails
            .modify(_.memberNino)
            .setTo(Some(validFormData))
          memberProvideDetailsService
            .upsert(updatedApplication)
            .map: _ =>
              Redirect(AppRoutes.providedetails.MemberSaUtrController.show.url)
      .redirectIfSaveForLater
