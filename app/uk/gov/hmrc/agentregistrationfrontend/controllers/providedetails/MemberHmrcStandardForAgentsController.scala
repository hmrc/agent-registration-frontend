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
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.MemberProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.llp.MemberProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.memberconfirmation.MemberHmrcStandardForAgentsPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberHmrcStandardForAgentsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: MemberHmrcStandardForAgentsPage,
  memberProvideDetailsService: MemberProvideDetailsService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[MemberProvideDetailsRequest, AnyContent] = actions.Member.getProvideDetailsInProgress
    .ensure(
      _.memberProvidedDetails.memberSaUtr.isDefined,
      implicit request =>
        Redirect(AppRoutes.providedetails.MemberSaUtrController.show)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view())

  def submit: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        memberProvideDetailsService
          .upsert(
            request.memberProvidedDetails
              .modify(_.hmrcStandardForAgentsAgreed)
              .setTo(StateOfAgreement.Agreed)
          ).map: _ =>
            Redirect(AppRoutes.providedetails.CheckYourAnswersController.show)
