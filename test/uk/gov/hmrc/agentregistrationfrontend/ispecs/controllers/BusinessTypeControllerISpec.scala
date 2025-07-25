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
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.agentregistrationfrontend.ispecs.ISpec

class BusinessTypeControllerISpec
  extends ISpec :

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl = s"http://localhost:${port.toString}/agent-registration"

  "GET /register should redirect to business type page" in :
    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/register")
        .withFollowRedirects(false)
        .get()
        .futureValue

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "/agent-registration/register/about-your-application/business-type"
  

  "GET /register/about-your-application/business-type should return 200 and render page" in :
    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/register/about-your-application/business-type")
        .withFollowRedirects(false)
        .get()
        .futureValue

    response.status shouldBe 200
    val content = response.body[String]
    content should include("How is your business set up?")
    content should include("Save and continue")

  "POST /register/about-your-application/business-type with valid selection should redirect to the next page" in :
    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/register/about-your-application/business-type")
        .withFollowRedirects(false)
        .post(Map("businessType" -> Seq("sole-trader")))
        .futureValue

    response.status shouldBe 303
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "/agent-registration/register/about-your-application/user-role"
  
  "POST /register/about-your-application/business-type without valid selection should return 400" in :
    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/register/about-your-application/business-type")
        .withFollowRedirects(false)
        .post(Map("businessType" -> Seq("")))
        .futureValue

    response.status shouldBe 400
    val content = response.body[String]
    content should include("There is a problem")
    content should include("Tell us how your business is set up")
