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
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficerRole
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object CompaniesHouseStubs {

  def stubSixOfficers(
    officerRole: CompaniesHouseOfficerRole = CompaniesHouseOfficerRole.LlpMember
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/companies-house-api-proxy/company/1234567890/officers"),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "total_results" -> 6,
        "items_per_page" -> 35,
        "etag" -> "c3b8d15615b770cd4fcc27fdc1c959474ae4c03e",
        "active_count" -> 6,
        "kind" -> "officer-list",
        "start_index" -> 0,
        "resigned_count" -> 1,
        "links" -> Json.obj(
          "self" -> "/company/1234567890/appointments"
        ),
        "items" -> Json.arr(
          Json.obj(
            "name" -> "Tester, John",
            "date_of_birth" -> Json.obj("month" -> 8, "year" -> 1967),
            "officer_role" -> officerRole.role
          ),
          Json.obj(
            "name" -> "Tester, John Ian",
            "date_of_birth" -> Json.obj("month" -> 4, "year" -> 1948),
            "officer_role" -> officerRole.role
          ),
          Json.obj(
            "name" -> "Tester, Alice",
            "date_of_birth" -> Json.obj("month" -> 1, "year" -> 1975),
            "officer_role" -> officerRole.role
          ),
          Json.obj(
            "name" -> "Tester, Bob",
            "date_of_birth" -> Json.obj("month" -> 12, "year" -> 1982),
            "officer_role" -> officerRole.role
          ),
          Json.obj(
            "name" -> "Tester, Carol",
            "date_of_birth" -> Json.obj("month" -> 6, "year" -> 1991),
            "officer_role" -> officerRole.role
          ),
          Json.obj(
            "name" -> "Tester, Carol",
            "date_of_birth" -> Json.obj("month" -> 6, "year" -> 1991),
            "officer_role" -> officerRole.role
          ),
          Json.obj(
            "name" -> "Tester, Dave (Resigned)",
            "date_of_birth" -> Json.obj("month" -> 2, "year" -> 1980),
            "resigned_on" -> "2024-01-01",
            "officer_role" -> officerRole.role
          ),
          Json.obj(
            "name" -> "Corporate Entity",
            "identification" -> Json.obj(
              "identification_type" -> "corporate-entity"
            ),
            "officer_role" -> officerRole.role
          )
        )
      ).toString
  )

  def verifySixOfficersCalls(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/companies-house-api-proxy/company/1234567890/officers"),
    count = count
  )

  /** We include a corporate entity in this response to verify that the service correctly filters out non-natural persons.
    */
  def stubZeroOfficers(): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/companies-house-api-proxy/company/1234567890/officers"),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "items" -> Json.arr(
          Json.obj(
            "name" -> "Corporate Entity",
            "identification" -> Json.obj(
              "identification_type" -> "corporate-entity"
            ),
            "officer_role" -> "general-partner-in-a-limited-partnership"
          )
        )
      ).toString
  )

  def stubSingleMatch(
    lastName: String,
    officerRole: CompaniesHouseOfficerRole = CompaniesHouseOfficerRole.LlpMember
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/companies-house-api-proxy/company/1234567890/officers\\?surname=$lastName"),
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
            ),
            "officer_role" -> officerRole.role
          )
        )
      ).toString
  )

  def stubFiveOrLess(
    name: String,
    officerRole: CompaniesHouseOfficerRole = CompaniesHouseOfficerRole.LlpMember
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/companies-house-api-proxy/company/1234567890/officers"),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "items" -> Json.arr(
          Json.obj(
            "name" -> s"$name",
            "date_of_birth" -> Json.obj(
              "day" -> 12,
              "month" -> 11,
              "year" -> 1990
            ),
            "officer_role" -> officerRole.role
          ),
          Json.obj(
            "name" -> s"Resigned Alt $name",
            "date_of_birth" -> Json.obj("month" -> 2, "year" -> 1980),
            "resigned_on" -> "2024-01-01",
            "officer_role" -> officerRole.role
          )
        )
      ).toString
  )

  def verifySingleMatchCalls(
    lastName: String,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/companies-house-api-proxy/company/1234567890/officers\\?surname=$lastName"),
    count = count
  )

  def verifyMultipleMatchCalls(
    lastName: String,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/companies-house-api-proxy/company/1234567890/officers\\?surname=$lastName"),
    count = count
  )

  def verifyMultipleMatchesWithResignedOfficerCalls(
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/companies-house-api-proxy/company/1234567890/officers"),
    count = count
  )

}
