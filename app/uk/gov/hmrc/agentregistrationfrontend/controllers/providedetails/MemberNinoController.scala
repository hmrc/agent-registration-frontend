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
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.MemberProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.MemberNinoForm
import uk.gov.hmrc.agentregistrationfrontend.services.llp.MemberProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
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

  private val baseAction: ActionBuilder[MemberProvideDetailsRequest, AnyContent] = actions.getProvideDetailsInProgress
    .ensure(
      mpd =>
        mpd.memberProvidedDetails.memberNino.isEmpty ||
          mpd.memberProvidedDetails.memberNino.exists {
            case _: MemberNino.FromAuth => false
            case _: MemberNino.Provided => true
            case _ @MemberNino.NotProvided => true
          },
      implicit request =>
        Redirect(routes.MemberSaUtrController.show.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        MemberNinoForm.form
          .fill:
            request
              .memberProvidedDetails
              .memberNino
      ))

  def submit: Action[AnyContent] = baseAction
    .ensureValidForm[MemberNino](
      MemberNinoForm.form,
      implicit request => formWithErrors => Errors.throwBadRequestException(s"Unexpected errors in the FormType: $formWithErrors")
    )
    .async:
      implicit request: (MemberProvideDetailsRequest[AnyContent] & FormValue[MemberNino]) =>
        val validFormData: MemberNino = request.formValue
        val updatedApplication: MemberProvidedDetails = request
          .memberProvidedDetails
          .modify(_.memberNino)
          .setTo(Some(validFormData))
        memberProvideDetailsService
          .upsert(updatedApplication)
          .map: _ =>
            Redirect(routes.MemberSaUtrController.show.url)
