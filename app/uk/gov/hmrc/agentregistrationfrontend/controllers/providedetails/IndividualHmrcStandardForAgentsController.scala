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
import uk.gov.hmrc.agentregistrationfrontend.action.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.action.individual.llp.IndividualProvideDetailsRequest

import uk.gov.hmrc.agentregistrationfrontend.services.llp.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.individualconfirmation.IndividualHmrcStandardForAgentsPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndividualHmrcStandardForAgentsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: IndividualActions,
  view: IndividualHmrcStandardForAgentsPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[IndividualProvideDetailsRequest, AnyContent] = actions.getProvideDetailsInProgress
    .ensure(
      _.individualProvidedDetails.individualSaUtr.isDefined,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualSaUtrController.show)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view())

  def submit: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        individualProvideDetailsService
          .upsert(
            request.individualProvidedDetails
              .modify(_.hmrcStandardForAgentsAgreed)
              .setTo(StateOfAgreement.Agreed)
          ).map: _ =>
            Redirect(AppRoutes.providedetails.CheckYourAnswersController.show)
