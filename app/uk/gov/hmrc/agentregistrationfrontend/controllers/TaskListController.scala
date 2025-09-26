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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.TaskListPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskListController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  taskListPage: TaskListPage
)
extends FrontendController(mcc):

  /* Show the task list if we have a UTR from GRS, otherwise redirect to start of registration */
  val show: Action[AnyContent] = actions.getApplicationInProgress { implicit request =>
    if (request.agentApplication.utr.isDefined)
      Ok(taskListPage())
    else
      Redirect(routes.AgentApplicationController.startRegistration)
  }
