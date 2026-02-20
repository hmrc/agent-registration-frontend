/*
 * Copyright 2024 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object CitizenDetailsStub:

  def stubFindSaUtrAndDateOfBirth(
    nino: Nino,
    saUtr: SaUtr,
    firstName: Option[String] = None,
    lastName: Option[String] = None
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/citizen-details/nino/${nino.value}"),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "name" -> Json.obj(
          "current" -> Json.obj(
            "firstName" -> firstName.getOrElse("John"),
            "lastName" -> lastName.getOrElse("Smith")
          )
        ),
        "dateOfBirth" -> "01012000",
        "ids" -> Json.obj(
          "sautr" -> saUtr.value
        )
      ).toString
  )

  def verifyFind(
    nino: Nino,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlPathEqualTo(s"/citizen-details/nino/${nino.value}"),
    count = count
  )

  def stubDesignatoryDetailsFound(
    nino: Nino,
    deceased: Boolean = false
  ): StubMapping =

    StubMaker.make(
      httpMethod = StubMaker.HttpMethod.GET,
      urlPattern = urlMatching(s"/citizen-details/${nino.value}/designatory-details"),
      responseStatus = 200,
      responseBody =
        Json.obj(
          "etag" -> "115",
          "person" -> Json.obj(
            "firstName" -> "HIPPY",
            "middleName" -> "T",
            "lastName" -> "SMITH",
            "title" -> "Mr",
            "honours" -> "BSC",
            "sex" -> "M",
            "dateOfBirth" -> "1952-04-01",
            "nino" -> nino.value,
            "deceased" -> deceased
          ),
          "address" -> Json.obj(
            "line1" -> "26 FARADAY DRIVE",
            "line2" -> "PO BOX 45",
            "line3" -> "LONDON",
            "postcode" -> "CT1 1RQ",
            "startDate" -> "2009-08-29",
            "country" -> "GREAT BRITAIN",
            "type" -> "Residential"
          )
        ).toString
    )

  def stubDesignatoryDetailsNotFound(
    nino: Nino,
    deceased: Boolean = false
  ): StubMapping =

    StubMaker.make(
      httpMethod = StubMaker.HttpMethod.GET,
      urlPattern = urlMatching(s"/citizen-details/${nino.value}/designatory-details"),
      responseStatus = 404,
      responseBody =
        Json.obj(
          "code" -> "INVALID_NINO",
          "message" -> s"Provided NINO $nino is not valid"
        ).toString
    )

  def verifyDesignatoryDetails(
    nino: Nino,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlPathEqualTo(s"/citizen-details/${nino.value}/designatory-details"),
    count = count
  )
