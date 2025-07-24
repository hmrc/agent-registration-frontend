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

package uk.gov.hmrc.agentregistrationfrontend.controllers

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.forms.ConfirmationForm
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.UserRolePage

import javax.inject.{Inject, Singleton}

@Singleton
class UserRoleController @Inject()(
  actions: Actions,
  mcc: MessagesControllerComponents,
  view: UserRolePage
)
extends FrontendController(mcc):

  def show: Action[AnyContent] = Action { implicit request =>
    val form = ConfirmationForm.form("userRole")
    Ok(view(form))
  }

  def submit: Action[AnyContent] = Action { implicit request =>
    ConfirmationForm.form("userRole").bindFromRequest().fold(
      formWithErrors => BadRequest(view(formWithErrors)),
      hasOwnership => {
        Redirect("routes.TODO").addingToSession(
          "hasOwnership" -> hasOwnership.toString
        )
      }
    )
  }
