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
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseNameQueryForm
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.LlpMemberNamePage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlpMemberNameController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  llpMemberNamePage: LlpMemberNamePage
)
extends FrontendController(mcc, actions):

  // TODO: this is a dumb controller to enable dev of the template and form
  def show: Action[AnyContent] = Action:
    implicit request =>
      Ok(llpMemberNamePage(CompaniesHouseNameQueryForm.form))

  def submit: Action[AnyContent] = Action:
    implicit request =>
      val form = CompaniesHouseNameQueryForm.form.bindFromRequest()
      form.fold(
        formWithErrors => BadRequest(llpMemberNamePage(formWithErrors)),
        validFormData => Ok(s"Form submitted successfully with data: $validFormData")
      )
