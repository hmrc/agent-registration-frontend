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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object CompaniesHouseStubs {

  def stubSingleMatch(lastName: String): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/companies-house-api-proxy/company/1234567890/officers\\?surname=$lastName&register_view=true&register_type=llp_members"),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "items" -> Json.arr(
          Json.obj(
            "name" -> s"Taylor $lastName",
            "date_of_birth" -> Json.obj(
              "day" -> 12,
              "month" -> 11,
              "year" -> 1990
            )
          )
        )
      ).toString
  )

  def stubMultipleMatches(lastName: String): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/companies-house-api-proxy/company/1234567890/officers\\?surname=$lastName&register_view=true&register_type=llp_members"),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "items" -> Json.arr(
          Json.obj(
            "name" -> s"Taylor $lastName",
            "date_of_birth" -> Json.obj(
              "day" -> 12,
              "month" -> 11,
              "year" -> 1990
            )
          ),
          Json.obj(
            "name" -> s"First Alt $lastName",
            "date_of_birth" -> Json.obj("month" -> 2, "year" -> 1980)
          )
        )
      ).toString
  )

}
