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
import uk.gov.hmrc.agentregistrationfrontend.ispecs.ISpec

class UserRoleControllerISpec
extends ISpec:

  private val userRolePath = s"/agent-registration/register/about-your-application/user-role"


  "GET /register/about-your-application/user-role should return 200 and render page" ignore:
    val response: WSResponse =
      get(userRolePath)

    response.status shouldBe 200
    val content = response.body[String]
    content should include("Are you the owner of the business?")
    content should include("Save and continue")

  "POST /register/about-your-application/user-role with valid selection should redirect to the next page" ignore:
    val response: WSResponse =
      post(userRolePath)(Map("userRole" -> Seq("false")))

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "/agent-registration/register/about-your-application/user-role"

  "POST /register/about-your-application/user-role without valid selection should return 400" ignore:
    val response: WSResponse =
      post(userRolePath)(Map("userRole" -> Seq("")))

    response.status shouldBe 400
    val content = response.body[String]
    content should include("There is a problem")
    content should include("Select ‘yes’ if you are the owner of the business")
