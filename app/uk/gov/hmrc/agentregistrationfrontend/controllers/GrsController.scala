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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.services.GrsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class GrsController @Inject()(mcc: MessagesControllerComponents,
                              grsService: GrsService,
                              appConfig: AppConfig)
                             (implicit ec: ExecutionContext)
  extends FrontendController(mcc) {

  //isTransactor == the person at the keyboard is registering on behalf of the entity and is not the owner (maybe need better name?)

  //isTransactor and party type should come from the session cache

  //sole-trader journey can double as a way to collect personal data on the registrant
  //sole-trader-id allows usage of individual journey without sautr check to collect personal info
  //where registrant and transactor are different people we can use the individual journey
  //to collect the transactor personal details as well

  def startJourney(partyType: String, isTransactor: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>
      val callbackUrl = appConfig.grsJourneyCallbackUrl(partyType)

      grsService
        .createJourney(partyType, isTransactor, callbackUrl)
        .map(Redirect(_))
  }

  def startRegistrantJourney(isTransactor: Boolean): Action[AnyContent] = Action.async {
    implicit request =>
      val callbackUrl = appConfig.grsRegistrantCallbackUrl

      grsService
        .createIndividualJourney(isTransactor, callbackUrl)
        .map(Redirect(_))
  }

  def startTransactorJourney: Action[AnyContent] = Action.async {
    implicit request =>
      val callbackUrl = appConfig.grsTransactorCallbackUrl

      grsService
        .createIndividualJourney(isTransactor = false, callbackUrl)
        .map(Redirect(_))
  }

  def journeyCallback(partyType: String, journeyId: String): Action[AnyContent] = Action.async {
    implicit request =>
      grsService
        .getJourneyDetails(Some(partyType), journeyId)
        .map(json => Ok(Json.prettyPrint(json)))
  }


  def registrantJourneyCallback(journeyId: String): Action[AnyContent] = Action.async {
    implicit request =>
      grsService
        .getJourneyDetails(None, journeyId)
        .map(json => Ok(Json.prettyPrint(json)))
  }


  def transactorJourneyCallback(journeyId: String): Action[AnyContent] = Action.async {
    implicit request =>
      grsService
        .getJourneyDetails(None, journeyId)
        .map(json => Ok(Json.prettyPrint(json)))
  }
}
