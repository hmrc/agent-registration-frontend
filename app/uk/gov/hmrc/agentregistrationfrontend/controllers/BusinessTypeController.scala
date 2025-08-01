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

import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.forms.BusinessTypeForm
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.BusinessTypePage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future
import com.softwaremill.quicklens._

@Singleton
class BusinessTypeController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  view: BusinessTypePage,
  applicationService: ApplicationService
)
extends FrontendController(mcc):

  def show: Action[AnyContent] = actions.getApplicationInProgress:
    implicit request =>
      val form: Form[BusinessType] =
        request
          .agentApplication
          .aboutYourApplication
          .businessType
          .fold(BusinessTypeForm.form)((businessType: BusinessType) =>
            BusinessTypeForm.form.fill(businessType)
          )
      Ok(view(form))

  def submit: Action[AnyContent] = actions.getApplicationInProgress.async:
    implicit request =>
      BusinessTypeForm.form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        (businessType: BusinessType) =>
          applicationService
            .upsert(
              request
                .agentApplication
                .modify(_.aboutYourApplication.businessType)
                .setTo(Some(businessType))
            )
            .map(_ => Redirect(routes.UserRoleController.show.url))
      )
