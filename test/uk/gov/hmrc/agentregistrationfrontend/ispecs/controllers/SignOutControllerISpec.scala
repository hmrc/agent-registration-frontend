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

package uk.gov.hmrc.agentregistrationfrontend.ispecs.controllers

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistrationfrontend.ispecs.ISpec

class SignOutControllerISpec
extends ISpec:

  private val baseUrl = s"http://localhost:${port.toString}/agent-registration"
  private val signOutPath = "/agent-registration/sign-out"
  private val timeOutPath = "/agent-registration/time-out"
  private val timedOutPath = "/agent-registration/timed-out"
  private val selfExternalUrl = "http://localhost:22201/agent-registration"
  private val signOutViaBasGatewayUrl = uri"http://localhost:9099/bas-gateway/sign-out-without-state"

  private def signOutWithContinue(continue: String): String = uri"$signOutViaBasGatewayUrl?${Map("continue" -> continue)}".toString

  "GET /sign-out" in:
    val response: WSResponse = get(signOutPath)

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe signOutWithContinue(selfExternalUrl)

  "GET /time-out" in:
    val timedOutUrl = uri"$selfExternalUrl/timed-out"
    val response: WSResponse = get(timeOutPath)

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe signOutWithContinue(timedOutUrl.toString)

  "GET /timed-out" in:
    val response: WSResponse = get(timedOutPath)

    response.status shouldBe 200
    response.body[String] should include("You have been signed out")
    response.body[String] should include("Sign in again")
