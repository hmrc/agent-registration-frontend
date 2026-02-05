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

package uk.gov.hmrc.agentregistrationfrontend.testonly.controllers

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.DefaultActionBuilder
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendControllerBase
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.TestOnlyHubPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestOnlyController @Inject() (
  mcc: MessagesControllerComponents,
  defaultActionBuilder: DefaultActionBuilder,
  testOnlyHubPage: TestOnlyHubPage
)
extends FrontendControllerBase(mcc):

  def showTestOnlyHub: Action[AnyContent] = defaultActionBuilder:
    implicit request =>
      Ok(testOnlyHubPage())

  def showPlaySession: Action[AnyContent] = defaultActionBuilder: request =>
    Ok(Json.prettyPrint(Json.toJson(request.session.data)))
