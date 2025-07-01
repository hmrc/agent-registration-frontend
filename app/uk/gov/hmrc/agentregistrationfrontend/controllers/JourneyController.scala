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
import uk.gov.hmrc.agentregistrationfrontend.journey.JourneyService
import uk.gov.hmrc.agentregistrationfrontend.views.Views
import uk.gov.hmrc.agentregistrationfrontend.views.html.HelloWorldPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class JourneyController @Inject()(
  mcc: MessagesControllerComponents,
  journeyService: JourneyService,
  views: Views
  )
    extends FrontendController(mcc) {

  val initializeJourney: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(views.helloWorldPage()))
  }

  val tryToRestoreJourney: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(views.helloWorldPage()))
  }
}
