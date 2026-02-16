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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions

import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage

import javax.inject.Inject

class IndividualConfirmStopController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  placeholder: SimplePage
)
extends FrontendController(mcc, actions):

  // This page is almost certainly never going to be used, not deleting yet until we know how to resolve the No answer in the
  // approve applicant form, it's most likely going to not have a YesNo form and instead just be a continue button to carry on instead of explicitly
  // begin able to stop anyone from completing the application - users can just choose not to continue if they don't approve instead of
  // being able to prevent others from completing their part of the application.
  def show: Action[AnyContent] = action:
    implicit request =>
      Ok(placeholder(
        h1 = "Confirm Stop Page",
        bodyText = Some("This is a placeholder for stop apllication  page.")
      ))
