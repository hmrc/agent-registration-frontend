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

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

import java.net.URLEncoder

class SignOutControllerSpec
extends ControllerSpec:

  private val signOutWithContinuePath = "/agent-registration/sign-out-with-continue"
  private val signOutPath = "/agent-registration/sign-out"
  private val timeOutPath = "/agent-registration/time-out"
  private val timedOutUrl = AppRoutes.SignOutController.timedOut.url
  private val landingUrl = AppRoutes.apply.AgentApplicationController.landing.url

  private def signOutWithContinue(continue: String): String = s"$signOutWithContinuePath?continueUrl=${URLEncoder.encode(continue, "UTF-8")}"

  "GET /sign-out" in:
    val response: WSResponse = get(signOutPath)
    val expectedContinueUrl = uri"${thisFrontendBaseUrl + landingUrl}"
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header(
      "Location"
    ).value shouldBe signOutWithContinue(expectedContinueUrl.toString)

  "GET /time-out" in:
    val expectedContinueUrl = uri"${thisFrontendBaseUrl + timedOutUrl}"
    val response: WSResponse = get(timeOutPath)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header(
      "Location"
    ).value shouldBe signOutWithContinue(expectedContinueUrl.toString)

  "GET /timed-out" in:
    val response: WSResponse = get(timedOutUrl)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "You have been signed out - Apply for an agent services account - GOV.UK"
