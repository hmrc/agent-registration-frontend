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

package uk.gov.hmrc.agentregistrationfrontend.ispecs

import play.api.libs.ws.{WSClient, WSResponse}
import play.api.libs.ws.DefaultBodyReadables.*

class HealthEndpointISpec
  extends ISpec:

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl = s"http://localhost:${port.toString}"

  "service health endpoint should respond with 200 status" in :
    val response: WSResponse =
      wsClient
        .url(s"$baseUrl/ping/ping")
        .get()
        .futureValue

    response.status shouldBe 200
    response.body[String] shouldBe ""
