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
import uk.gov.hmrc.agentregistration.shared.Crn
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
          )
        )
      ).toString
  )

  def verifySixOfficersCalls(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/companies-house-api-proxy/company/1234567890/officers"),
    count = count
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

  def givenUnsuccessfulGetCompanyHouseResponse(
    crn: Crn,
    statusResponse: Int
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/companies-house-api-proxy/company/${crn.value}"),
    responseStatus = statusResponse,
    responseBody = ""
  )

  def givenSuccessfulGetCompanyHouseResponse(
    crn: Crn,
    companyStatus: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/companies-house-api-proxy/company/${crn.value}"),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "accounts" -> Json.obj(
          "accounting_reference_date" -> Json.obj(
            "day" -> 1,
            "month" -> 1
          ),
          "last_accounts" -> Json.obj(
            "made_up_to" -> "2024-01-01",
            "type" -> "string"
          ),
          "next_due" -> "2025-01-01",
          "next_made_up_to" -> "2025-01-01",
          "overdue" -> false
        ),
        "annual_return" -> Json.obj(
          "last_made_up_to" -> "2024-01-01",
          "next_due" -> "2025-01-01",
          "next_made_up_to" -> "2025-01-01",
          "overdue" -> false
        ),
        "branch_company_details" -> Json.obj(
          "business_activity" -> "string",
          "parent_company_name" -> "string",
          "parent_company_number" -> "string"
        ),
        "can_file" -> true,
        "company_name" -> "Watford Microbreweries",
        "company_number" -> crn.value,
        "company_status" -> companyStatus,
        "company_status_detail" -> "string",
        "confirmation_statement" -> Json.obj(
          "last_made_up_to" -> "2024-01-01",
          "next_due" -> "2025-01-01",
          "next_made_up_to" -> "2025-01-01",
          "overdue" -> false
        ),
        "date_of_creation" -> "2020-01-01",
        "date_of_dissolution" -> "2024-01-01",
        "etag" -> "string",
        "foreign_company_details" -> Json.obj(
          "accounting_requirement" -> Json.obj(
            "foreign_account_type" -> "string",
            "terms_of_account_publication" -> "string"
          ),
          "accounts" -> Json.obj(
            "account_period_from" -> Json.obj("day" -> 1, "month" -> 1),
            "account_period_to" -> Json.obj("day" -> 31, "month" -> 12),
            "must_file_within" -> Json.obj("months" -> 9)
          ),
          "business_activity" -> "string",
          "company_type" -> "string",
          "governed_by" -> "string",
          "is_a_credit_finance_institution" -> false,
          "originating_registry" -> Json.obj(
            "country" -> "string",
            "name" -> "string"
          ),
          "registration_number" -> "string"
        ),
        "has_been_liquidated" -> false,
        "has_charges" -> false,
        "has_insolvency_history" -> false,
        "is_community_interest_company" -> false,
        "jurisdiction" -> "string",
        "last_full_members_list_date" -> "2024-01-01",
        "links" -> Json.obj(
          "persons_with_significant_control_list" -> "string",
          "persons_with_significant_control_statements_list" -> "string",
          "self" -> "string"
        ),
        "officer_summary" -> Json.obj(
          "active_count" -> 1,
          "officers" -> Json.arr(
            Json.obj(
              "appointed_on" -> "2020-01-01",
              "date_of_birth" -> Json.obj(
                "day" -> 23,
                "month" -> 4,
                "year" -> 1948
              ),
              "name" -> "Jim Ferguson",
              "officer_role" -> "director"
            )
          ),
          "resigned_count" -> 0
        ),
        "registered_office_address" -> Json.obj(
          "address_line_1" -> "string",
          "address_line_2" -> "string",
          "care_of" -> "string",
          "country" -> "string",
          "locality" -> "string",
          "po_box" -> "string",
          "postal_code" -> "string",
          "premises" -> "string",
          "region" -> "string"
        ),
        "registered_office_is_in_dispute" -> false,
        "sic_codes" -> Json.arr("string"),
        "type" -> "string",
        "undeliverable_registered_office_address" -> false
      ).toString
  )

  def verifyGetCompanyHouse(
    crn: Crn,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/companies-house-api-proxy/company/${crn.value}"),
    count = count
  )

}
