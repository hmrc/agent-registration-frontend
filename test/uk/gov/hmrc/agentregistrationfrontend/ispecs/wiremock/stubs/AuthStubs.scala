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

package uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock.StubMaker
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll

object AuthStubs {

  def stubAuthorise(
    responseBody: String = responseBodyAsCleanAgent()
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching("/auth/authorise"),
    requestBody = Some(expectedRequestBody),
    responseStatus = Status.OK,
    responseBody = responseBody
  )

  def verifyAuthorise(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching("/auth/authorise"),
    count = count
  )

  def responseBodyAsCleanAgent(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId,
    groupId: GroupId = TdAll.tdAll.groupId
  ): String =
    // language=JSON
    s"""
       |{
       |  "authorisedEnrolments": [],
       |  "allEnrolments": [],
       |  "credentialRole": "User",
       |  "groupIdentifier": "${groupId.value}",
       |  "agentInformation": {},
       |  "internalId": "${internalUserId.value}"
       |}
       |""".stripMargin

  private val expectedRequestBody: StringValuePattern = wm.equalToJson(
    // language=JSON
    """
      |{
      |  "authorise": [
      |    {
      |      "authProviders": [
      |        "GovernmentGateway"
      |      ]
      |    },
      |    {
      |      "affinityGroup": "Agent"
      |    }
      |  ],
      |  "retrieve": [
      |    "allEnrolments",
      |    "groupIdentifier",
      |    "credentialRole",
      |    "internalId"
      |  ]
      |}
      |""".stripMargin
  )

}
