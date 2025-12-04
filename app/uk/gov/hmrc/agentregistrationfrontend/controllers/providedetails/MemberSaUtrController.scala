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
import uk.gov.hmrc.agentregistration.shared.llp.MemberSaUtr
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.MemberProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.MemberSaUtrForm
import uk.gov.hmrc.agentregistrationfrontend.services.llp.MemberProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.memberconfirmation.MemberSaUtrPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberSaUtrController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  view: MemberSaUtrPage,
  memberProvideDetailsService: MemberProvideDetailsService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[MemberProvideDetailsRequest, AnyContent] = actions.Member.getProvideDetailsInProgress
    .ensure(
      mpd =>
        mpd.memberProvidedDetails.memberNino.nonEmpty &&
          mpd.memberProvidedDetails.memberSaUtr.exists {
            case MemberSaUtr.FromAuth(_) => false
            case MemberSaUtr.FromCitizenDetails(_) => false
            case _ => true
          },
      implicit request =>
        logger.info(s"SaUtr is already provided from auth or citizen details. Skipping page and moving to next page.")
        Redirect(AppRoutes.providedetails.MemberApproveApplicantController.show.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        MemberSaUtrForm.form
          .fill:
            request
              .memberProvidedDetails
              .memberSaUtr
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[MemberSaUtr](
        MemberSaUtrForm.form,
        implicit r => view(_)
      )
      .async:
        implicit request: (MemberProvideDetailsRequest[AnyContent] & FormValue[MemberSaUtr]) =>
          val validFormData: MemberSaUtr = request.formValue
          val updatedApplication: MemberProvidedDetails = request
            .memberProvidedDetails
            .modify(_.memberSaUtr)
            .setTo(Some(validFormData))
          memberProvideDetailsService
            .upsert(updatedApplication)
            .map: _ =>
              Redirect(AppRoutes.providedetails.MemberApproveApplicantController.show.url)
      .redirectIfSaveForLater
