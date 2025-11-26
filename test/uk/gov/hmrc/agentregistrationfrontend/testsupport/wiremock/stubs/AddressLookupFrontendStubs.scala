/*
 * Copyright 2017 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.AddressLookupFrontendAddress
import uk.gov.hmrc.agentregistrationfrontend.model.addresslookup.JourneyId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object AddressLookupFrontendStubs:

  def makeJourneyConfig(continueUrl: String): JsObject = Json.obj(
    "version" -> 2,
    "options" -> Json.obj(
      "continueUrl" -> s"$continueUrl",
      "includeHMRCBranding" -> true,
      "signOutHref" -> "http://localhost:22201/agent-registration/sign-out",
      "selectPageConfig" -> Json.obj(
        "proposedListLimit" -> 30,
        "showSearchLinkAgain" -> true
      ),
      "allowedCountryCodes" -> Json.arr("GB"),
      "confirmPageConfig" -> Json.obj(
        "showChangeLink" -> true,
        "showSubHeadingAndInfo" -> true,
        "showSearchAgainLink" -> false,
        "showConfirmChangeText" -> true
      ),
      "timeoutConfig" -> Json.obj(
        "timeoutAmount" -> 900,
        "timeoutUrl" -> "http://localhost:22201/agent-registration/time-out"
      )
    ),
    "labels" -> Json.obj(
      "en" -> Json.obj(
        "appLevelLabels" -> Json.obj(
          "navTitle" -> "Apply for an agent services account"
        ),
        "lookupPageLabels" -> Json.obj(
          "title" -> "What correspondence address should we use for your agent services account? - Apply for an agent services account - GOV.UK",
          "heading" -> "What correspondence address should we use for your agent services account?"
        ),
        "editPageLabels" -> Json.obj(
          "title" -> "Change your address - Apply for an agent services account - GOV.UK",
          "heading" -> "Change your address"
        )
      ),
      "cy" -> Json.obj(
        "appLevelLabels" -> Json.obj(
          "navTitle" -> "Apply for an agent services account"
        ),
        "lookupPageLabels" -> Json.obj(
          "title" -> "What correspondence address should we use for your agent services account? - Apply for an agent services account - GOV.UK",
          "heading" -> "What correspondence address should we use for your agent services account?"
        ),
        "editPageLabels" -> Json.obj(
          "title" -> "Change your address - Apply for an agent services account - GOV.UK",
          "heading" -> "Change your address"
        )
      )
    )
  )

  def stubAddressLookupInit(continueUrl: String): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlMatching("/api/v2/init"),
    responseStatus = 202,
    requestBody = Some(wm.equalToJson(s"${makeJourneyConfig(continueUrl)}")),
    responseHeaders = Seq(HeaderNames.LOCATION -> "http://localhost:9028/any-uri-determined-by-alf")
  )

  def stubAddressLookupWithId(
    journeyId: JourneyId,
    address: AddressLookupFrontendAddress
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/api/confirmed\\?id=${journeyId.value}"),
    responseStatus = 200,
    responseBody = Json.obj("address" -> Json.toJson(address)).toString
  )

  def verifyAddressLookupInit(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlMatching("/api/v2/init"),
    count = count
  )

  def verifyAddressLookupWithId(
    journeyId: JourneyId,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/api/confirmed\\?id=${journeyId.value}"),
    count = count
  )
