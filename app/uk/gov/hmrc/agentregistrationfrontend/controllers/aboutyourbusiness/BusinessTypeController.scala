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

package uk.gov.hmrc.agentregistrationfrontend.controllers.aboutyourbusiness

import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.BusinessTypeForm
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.aboutyourbusiness.BusinessTypePage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessTypeController @Inject() (
  mcc: MessagesControllerComponents,
  view: BusinessTypePage
)
extends FrontendController(mcc):

  def show: Action[AnyContent] = Action:
    implicit request =>
      val form: Form[BusinessType] =
        request.readBusinessType match
          case Some(value: BusinessType) => BusinessTypeForm.form.fill(value)
          case _ => BusinessTypeForm.form
      Ok(view(form))

  def submit: Action[AnyContent] = Action:
    implicit request =>
      BusinessTypeForm.form.bindFromRequest().fold(
        formWithErrors => BadRequest(view(formWithErrors)),
        (businessType: BusinessType) =>
          Redirect("routes.UserRoleController.show.url")
            .addBusinessTypeToSession(businessType)
      )
