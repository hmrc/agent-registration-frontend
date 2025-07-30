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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.forms.BusinessTypeForm
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.BusinessTypePage

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future


@Singleton
class BusinessTypeController @Inject()(
                                        actions: Actions,
                                        mcc: MessagesControllerComponents,
                                        view: BusinessTypePage,
                                        applicationService: ApplicationService
                                      )
  extends FrontendController(mcc):

  def show: Action[AnyContent] = actions.getApplicationInProgress.async { implicit request =>
    val userAnswers = request.agentApplication.aboutYourApplication
    val form: Form[BusinessType] = if userAnswers.businessType.isDefined
    then
      BusinessTypeForm.form.fill(userAnswers.businessType.get)
    else
      BusinessTypeForm.form
    Future.successful(Ok(view(form)))
  }

  def submit: Action[AnyContent] = actions.getApplicationInProgress.async { implicit request =>
    BusinessTypeForm.form.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
      businessType => applicationService.upsert(
        agentApplication = request.agentApplication.copy(
          aboutYourApplication = request.agentApplication.aboutYourApplication.copy(businessType = Some(businessType))
        )).map { _ =>
        Redirect(routes.UserRoleController.show.url)
      }
    )
  }
