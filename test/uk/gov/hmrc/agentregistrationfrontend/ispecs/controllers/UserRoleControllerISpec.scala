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
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSResponse
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistrationfrontend.ispecs.ISpec

class UserRoleControllerISpec
extends ISpec:

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl = s"http://localhost:${port.toString}/agent-registration"

  // TODO - later when we have previous page
  /*
  "GET /XXX should redirect to user role page" in :
    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/XXX")
        .withFollowRedirects(false)
        .get()
        .futureValue

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "/agent-registration/register/about-your-application/user-role"

   */

  "GET /register/about-your-application/user-role should return 200 and render page" in:
    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/register/about-your-application/user-role")
        .withFollowRedirects(false)
        .get()
        .futureValue

    response.status shouldBe 200
    val content = response.body[String]
    content should include("Are you the owner of the business?")
    content should include("Save and continue")

  "POST /register/about-your-application/user-role with valid selection should redirect to the next page" in:
    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/register/about-your-application/user-role")
        .withFollowRedirects(false)
        .post(Map("userRole" -> Seq("false")))
        .futureValue

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "routes.TODO"

  "POST /register/about-your-application/user-role without valid selection should return 400" in:
    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/register/about-your-application/user-role")
        .withFollowRedirects(false)
        .post(Map("userRole" -> Seq("")))
        .futureValue

    response.status shouldBe 400
    val content = response.body[String]
    content should include("There is a problem")
    content should include("Select ‘yes’ if you are the owner of the business")
