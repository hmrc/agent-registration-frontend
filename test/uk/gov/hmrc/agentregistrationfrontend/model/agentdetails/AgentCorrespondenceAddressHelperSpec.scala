/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.model.agentdetails

import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentCorrespondenceAddress
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.addresslookup.Country
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.addresslookup.GetConfirmedAddressResponse
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.agentdetails.AgentCorrespondenceAddressHelper
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.agentdetails.AgentCorrespondenceAddressHelper.toValueString
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec

class AgentCorrespondenceAddressHelperSpec
extends UnitSpec:

  "toValueString" should:
    toValueStringTestCases.foreach: tc =>
      s"${tc.testCaseName}" in:
        tc.agentCorrespondenceAddress.toValueString shouldBe tc.expected

  "fromValueString" should:
    fromValueStringTestCases.foreach: tc =>
      s"${tc.testCaseName}" in:
        AgentCorrespondenceAddressHelper.fromValueString(tc.agentCorrespondenceAddressCommaSeparated) shouldBe tc.agentCorrespondenceAddress
    fromValueStringErrorTestCases.foreach: tc =>
      s"${tc.testCaseName}" in:
        val exception = intercept[IllegalArgumentException]:
          AgentCorrespondenceAddressHelper.fromValueString(tc.input)
        exception.getMessage shouldBe s"Cannot parse CorrespondenceAddress from string: ${tc.input}"

  "fromAddressLookupAddress" should:
    fromAddressLookupAddress.foreach: tc =>
      s"${tc.testCaseName}" in:
        AgentCorrespondenceAddressHelper.fromAddressLookupAddress(tc.getConfirmedAddressResponse) shouldBe tc.expected

  final case class FromAddressLookupAddressTestCase(
    getConfirmedAddressResponse: GetConfirmedAddressResponse,
    expected: AgentCorrespondenceAddress,
    testCaseName: String
  )

  final case class FromValueStringTestCase(
    agentCorrespondenceAddressCommaSeparated: String,
    agentCorrespondenceAddress: AgentCorrespondenceAddress,
    testCaseName: String
  )

  lazy val fromValueStringTestCases: List[FromValueStringTestCase] = List(
    FromValueStringTestCase(
      agentCorrespondenceAddressCommaSeparated = "123 Test Street, Test Area, Test Town, Test County, AB12 3CD, GB",
      agentCorrespondenceAddress = AgentCorrespondenceAddress(
        addressLine1 = "123 Test Street",
        addressLine2 = Some("Test Area"),
        addressLine3 = Some("Test Town"),
        addressLine4 = Some("Test County"),
        postalCode = Some("AB12 3CD"),
        countryCode = "GB"
      ),
      testCaseName = "should parse a 6-part address string with all fields"
    ),
    FromValueStringTestCase(
      agentCorrespondenceAddressCommaSeparated = "456 Main Road, City Centre, Greater Area, XY98 7ZZ, IE",
      agentCorrespondenceAddress = AgentCorrespondenceAddress(
        addressLine1 = "456 Main Road",
        addressLine2 = Some("City Centre"),
        addressLine3 = Some("Greater Area"),
        addressLine4 = None,
        postalCode = Some("XY98 7ZZ"),
        countryCode = "IE"
      ),
      testCaseName = "should parse a 5-part address string without addressLine4"
    ),
    FromValueStringTestCase(
      agentCorrespondenceAddressCommaSeparated = "789 High Street, Town Name, CD45 6EF, GB",
      agentCorrespondenceAddress = AgentCorrespondenceAddress(
        addressLine1 = "789 High Street",
        addressLine2 = Some("Town Name"),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("CD45 6EF"),
        countryCode = "GB"
      ),
      testCaseName = "should parse a 4-part address string with only addressLine1 and addressLine2"
    ),
    FromValueStringTestCase(
      agentCorrespondenceAddressCommaSeparated = "  123 Test Street  ,  Test Area  ,  Test Town  ,  AB12 3CD  ,  GB  ",
      agentCorrespondenceAddress = AgentCorrespondenceAddress(
        addressLine1 = "123 Test Street",
        addressLine2 = Some("Test Area"),
        addressLine3 = Some("Test Town"),
        addressLine4 = None,
        postalCode = Some("AB12 3CD"),
        countryCode = "GB"
      ),
      testCaseName = "should handle whitespace around parts correctly"
    )
  )

  final case class FromValueStringErrorTestCase(
    input: String,
    testCaseName: String
  )

  lazy val fromValueStringErrorTestCases: List[FromValueStringErrorTestCase] = List(
    FromValueStringErrorTestCase(
      input = "123 Test Street, Test Area, GB",
      testCaseName = "should throw IllegalArgumentException for string with fewer than 4 parts"
    ),
    FromValueStringErrorTestCase(
      input = "Part1, Part2, Part3, Part4, Part5, Part6, Part7",
      testCaseName = "should throw IllegalArgumentException for string with more than 6 parts"
    ),
    FromValueStringErrorTestCase(
      input = "",
      testCaseName = "should throw IllegalArgumentException for empty string"
    )
  )

  lazy val fromAddressLookupAddress: List[FromAddressLookupAddressTestCase] = List(
    FromAddressLookupAddressTestCase(
      getConfirmedAddressResponse = GetConfirmedAddressResponse(
        lines = Seq(
          "123 Test Street",
          "Test Area",
          "Test Town",
          "Test County"
        ),
        postcode = Some("AB12 3CD"),
        country = Country(code = "GB", name = Some("United Kingdom"))
      ),
      expected = AgentCorrespondenceAddress(
        addressLine1 = "123 Test Street",
        addressLine2 = Some("Test Area"),
        addressLine3 = Some("Test Town"),
        addressLine4 = Some("Test County"),
        postalCode = Some("AB12 3CD"),
        countryCode = "GB"
      ),
      testCaseName = "should map all four address lines with postcode"
    ),
    FromAddressLookupAddressTestCase(
      getConfirmedAddressResponse = GetConfirmedAddressResponse(
        lines = Seq("456 Main Road"),
        postcode = Some("XY98 7ZZ"),
        country = Country(code = "GB", name = Some("United Kingdom"))
      ),
      expected = AgentCorrespondenceAddress(
        addressLine1 = "456 Main Road",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("XY98 7ZZ"),
        countryCode = "GB"
      ),
      testCaseName = "should handle single line address"
    ),
    FromAddressLookupAddressTestCase(
      getConfirmedAddressResponse = GetConfirmedAddressResponse(
        lines = Seq("789 High Street", "City Centre"),
        postcode = None,
        country = Country(code = "IE", name = Some("Ireland"))
      ),
      expected = AgentCorrespondenceAddress(
        addressLine1 = "789 High Street",
        addressLine2 = Some("City Centre"),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = None,
        countryCode = "IE"
      ),
      testCaseName = "should handle address without postcode"
    ),
    FromAddressLookupAddressTestCase(
      getConfirmedAddressResponse = GetConfirmedAddressResponse(
        lines = Seq(),
        postcode = Some("XX11 2YY"),
        country = Country(code = "GB", name = None)
      ),
      expected = AgentCorrespondenceAddress(
        addressLine1 = "",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("XX11 2YY"),
        countryCode = "GB"
      ),
      testCaseName = "should handle empty lines with empty string for addressLine1"
    ),
    FromAddressLookupAddressTestCase(
      getConfirmedAddressResponse = GetConfirmedAddressResponse(
        lines = Seq(
          "10 Park Lane",
          "Business District",
          "Capital City"
        ),
        postcode = Some("CD45 6EF"),
        country = Country(code = "GB", name = Some("United Kingdom"))
      ),
      expected = AgentCorrespondenceAddress(
        addressLine1 = "10 Park Lane",
        addressLine2 = Some("Business District"),
        addressLine3 = Some("Capital City"),
        addressLine4 = None,
        postalCode = Some("CD45 6EF"),
        countryCode = "GB"
      ),
      testCaseName = "should handle three line address"
    )
  )

  final case class ToValueStringTestCase(
    agentCorrespondenceAddress: AgentCorrespondenceAddress,
    expected: String,
    testCaseName: String
  )

  lazy val toValueStringTestCases: List[ToValueStringTestCase] = List(
    ToValueStringTestCase(
      agentCorrespondenceAddress = AgentCorrespondenceAddress(
        addressLine1 = "123 Test Street",
        addressLine2 = Some("Test Area"),
        addressLine3 = Some("Test Town"),
        addressLine4 = Some("Test County"),
        postalCode = Some("AB12 3CD"),
        countryCode = "GB"
      ),
      expected = "123 Test Street, Test Area, Test Town, Test County, AB12 3CD, GB",
      testCaseName = "should render all fields as comma-separated string"
    ),
    ToValueStringTestCase(
      agentCorrespondenceAddress = AgentCorrespondenceAddress(
        addressLine1 = "456 Main Road",
        addressLine2 = Some("City Centre"),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("XY98 7ZZ"),
        countryCode = "IE"
      ),
      expected = "456 Main Road, City Centre, XY98 7ZZ, IE",
      testCaseName = "should omit empty optional fields"
    ),
    ToValueStringTestCase(
      agentCorrespondenceAddress = AgentCorrespondenceAddress(
        addressLine1 = "789 High Street",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postalCode = None,
        countryCode = "GB"
      ),
      expected = "789 High Street, GB",
      testCaseName = "should handle address with only addressLine1 and countryCode"
    ),
    ToValueStringTestCase(
      agentCorrespondenceAddress = AgentCorrespondenceAddress(
        addressLine1 = "12a, Baker Street",
        addressLine2 = Some("Flat 5, Block B"),
        addressLine3 = Some("London"),
        addressLine4 = None,
        postalCode = Some("NW1 6XE"),
        countryCode = "GB"
      ),
      expected = "12a Baker Street, Flat 5 Block B, London, NW1 6XE, GB",
      testCaseName = "should replace commas within address lines with spaces"
    ),
    ToValueStringTestCase(
      agentCorrespondenceAddress = AgentCorrespondenceAddress(
        addressLine1 = "10 Park Lane",
        addressLine2 = Some("Business District"),
        addressLine3 = Some("Capital City"),
        addressLine4 = Some("Greater Region"),
        postalCode = None,
        countryCode = "IE"
      ),
      expected = "10 Park Lane, Business District, Capital City, Greater Region, IE",
      testCaseName = "should handle address without postalCode"
    ),
    ToValueStringTestCase(
      agentCorrespondenceAddress = AgentCorrespondenceAddress(
        addressLine1 = "Unit 7, Suite 3, Floor 2",
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("AB1 2CD"),
        countryCode = "GB"
      ),
      expected = "Unit 7 Suite 3 Floor 2, AB1 2CD, GB",
      testCaseName = "should replace multiple commas in addressLine1"
    )
  )
