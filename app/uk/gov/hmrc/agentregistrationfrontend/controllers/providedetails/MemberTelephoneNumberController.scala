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

import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistrationfrontend.forms.MemberTelephoneNumberForm
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.memberconfirmation.MemberTelephoneNumberPage
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.MemberProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistrationfrontend.services.llp.MemberProvideDetailsService

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class MemberTelephoneNumberController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  view: MemberTelephoneNumberPage,
  memberProvideDetailsService: MemberProvideDetailsService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[MemberProvideDetailsRequest, AnyContent] = actions.getProvideDetailsInProgress
    .ensure(
      _.memberProvidedDetails.companiesHouseMatch.nonEmpty, // TODO: Add check for companies house details
      implicit request =>
        Redirect(routes.CompaniesHouseNameQueryController.show)
    )

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater(MemberTelephoneNumberForm.form, implicit r => view(_))
      .async:
        implicit request: MemberProvideDetailsRequest[AnyContent] =>
          MemberTelephoneNumberForm.form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
              telephoneNumberFromForm =>
                val updatedApplication: MemberProvidedDetails = request
                  .memberProvidedDetails
                  .modify(_.telephoneNumber)
                  .setTo(Some(telephoneNumberFromForm))
                memberProvideDetailsService
                  .upsert(updatedApplication)
                  .map: _ =>
                    Redirect(routes.MemberEmailAddressController.show.url)
            )
      .redirectIfSaveForLater

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        MemberTelephoneNumberForm.form
          .fill:
            request
              .memberProvidedDetails
              .telephoneNumber
      ))
