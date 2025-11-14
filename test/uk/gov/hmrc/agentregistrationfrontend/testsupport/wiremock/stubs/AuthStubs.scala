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

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object AuthStubs {

  def stubAuthorise(
    responseBody: String = responseBodyAsCleanAgent()
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching("/auth/authorise"),
    requestBody = Some(expectedRequestBodyAgent),
    responseStatus = Status.OK,
    responseBody = responseBody
  )

  def verifyAuthorise(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching("/auth/authorise"),
    count = count
  )

  def stubAuthoriseIndividual(
    responseBody: String = responseBodyAsIndividual()
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching("/auth/authorise"),
    requestBody = Some(expectedRequestBodyIndividual),
    responseStatus = Status.OK,
    responseBody = responseBody
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
       |  "internalId": "${internalUserId.value}",
       |  "optionalCredentials": {"providerId":"cred-id-12345","providerType":"GovernmentGateway"}
       |}
       |""".stripMargin

  private def responseBodyAsIndividual(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId
  ): String =
    Json.obj(
      "internalId" -> internalUserId.value,
      "affinityGroup" -> "Individual",
      "confidenceLevel" -> 250,
      "optionalCredentials" -> Json.obj(
        "providerId" -> "cred-id-12345",
        "providerType" -> "GovernmentGateway"
      ),
      "allEnrolments" -> Json.arr(Json.obj(
        "key" -> "MTD-IT",
        "identifiers" -> Json.arr(Json.obj(
          "key" -> "AnyIdentifier",
          "value" -> "AnyValue"
        ))
      ))
    ).toString

  private val expectedRequestBodyIndividual: StringValuePattern = wm.equalToJson(
    Json.obj(
      "authorise" -> Json.arr(
        Json.obj(
          "authProviders" -> Json.arr("GovernmentGateway")
        ),
        Json.obj(
          "affinityGroup" -> "Individual"
        )
      ),
      "retrieve" -> Json.arr(
        "allEnrolments",
        "internalId",
        "optionalCredentials"
      )
    ).toString
  )

  private val expectedRequestBodyAgent: StringValuePattern = wm.equalToJson(
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
      |    "internalId",
      |    "optionalCredentials"
      |  ]
      |}
      |""".stripMargin
  )

}
