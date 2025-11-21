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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker
import play.api.libs.json.Json

object CitizenDetailsStub {

  def stubFindSaUtr(
    nino: Nino,
    saUtr: SaUtr
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/citizen-details/nino/${nino.value}"),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "name" -> Json.obj(
          "current" -> Json.obj(
            "firstName" -> "John",
            "lastName" -> "Smith"
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

}
