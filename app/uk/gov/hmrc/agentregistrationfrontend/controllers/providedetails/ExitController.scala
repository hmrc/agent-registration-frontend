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

import play.api.mvc.*
import uk.gov.hmrc.agentregistrationfrontend.action.individual.Actions

import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExitController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  placeholderExitPage: SimplePage,
  multipleMemberProvidedDetailsPage: SimplePage
)
extends FrontendController(mcc, actions):

  def genericExitPage: Action[AnyContent] = actions.action:
    implicit request =>
      Ok(placeholderExitPage(
        h1 = "You cannot use this service...",
        bodyText = Some(
          "Placeholder for member provided detailsgeneric exit page..."
        )
      ))

  def multipleProvidedDetailsPage: Action[AnyContent] = actions.action:
    implicit request =>
      Ok(multipleMemberProvidedDetailsPage(
        h1 = "You cannot use this service...",
        bodyText = Some(
          "Placeholder for multiple member provided details generic page..."
        )
      ))
