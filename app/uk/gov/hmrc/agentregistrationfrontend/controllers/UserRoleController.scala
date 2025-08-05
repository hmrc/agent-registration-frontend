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

import com.softwaremill.quicklens.*
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.forms.UserRoleForm
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.UserRolePage

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class UserRoleController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  view: UserRolePage,
  applicationService: ApplicationService
)
extends FrontendController(mcc):

  def show: Action[AnyContent] = actions.getApplicationInProgress:
    implicit request =>
      val form: Form[UserRole] =
        request
          .agentApplication
          .aboutYourApplication
          .userRole
          .fold(UserRoleForm.form)((businessType: UserRole) =>
            UserRoleForm.form.fill(businessType)
          )
      Ok(view(form))

  def submit: Action[AnyContent] = actions.getApplicationInProgress.async:
    implicit request =>
      UserRoleForm.form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        (userRole: UserRole) =>
          applicationService
            .upsert(
              request
                .agentApplication
                .modify(_.aboutYourApplication.userRole)
                .setTo(Some(userRole))
            )
            .map(_ => Redirect("routes.TODO.checkYourAnswers"))
      )
