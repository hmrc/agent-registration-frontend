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
import uk.gov.hmrc.agentregistrationfrontend.action.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.action.individual.llp.IndividualProvideDetailsRequest

import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage

@Singleton
class IndividualCheckYourAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: IndividualActions,
  placeholderStartPage: SimplePage
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
      Ok(placeholderStartPage(
        h1 = "Start page for check your answers",
        bodyText = Some("Placeholder for check your answers")
      ))
