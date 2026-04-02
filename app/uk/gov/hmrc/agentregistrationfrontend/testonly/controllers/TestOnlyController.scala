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
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendControllerBase
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.PlanetId
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.UserId
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.StubUserService
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.TestOnlyHubPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.util.chaining.scalaUtilChainingOps

@Singleton
class TestOnlyController @Inject() (
  mcc: MessagesControllerComponents,
  defaultActionBuilder: DefaultActionBuilder,
  testOnlyHubPage: TestOnlyHubPage,
  stubUserService: StubUserService
)
extends FrontendControllerBase(mcc):

  def showTestOnlyHub: Action[AnyContent] = defaultActionBuilder:
    implicit request =>
      Ok(testOnlyHubPage())

  def showPlaySession: Action[AnyContent] = defaultActionBuilder: request =>
    Ok(Json.prettyPrint(Json.toJson(request.session.data)))

  def logIn(
    internalUserId: InternalUserId,
    redirectUrl: String
  ): Action[AnyContent] = defaultActionBuilder
    .async:
      implicit request =>
        val (userId: UserId, planetId: PlanetId) =
          internalUserId
            .value
            .split("@", 2) match
            case Array(left, right) => (UserId(left), PlanetId(right))
            case e => throw new IllegalArgumentException(s"Invalid internalUserId (it must be in format userId@planetId): $e")

        import StubUserService.addToSession
        for
          user <- stubUserService.findUser(userId, planetId).map(_.getOrThrowExpectedDataMissing("user"))
          loginResponse <- stubUserService.signIn(user)
        yield Redirect(redirectUrl).addToSession(loginResponse)
