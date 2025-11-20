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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object IndividualAuthStubs {

  def stubAuthorise(
    responseBody: String = responseBodyAsCleanAgent()
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching("/auth/authorise"),
    requestBody = Some(expectedRequestBody),
    responseStatus = Status.OK,
    responseBody = responseBody
  )

  def stubAuthoriseWithNino(
    responseBody: String = responseBodyAsAgentWithNino()
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching("/auth/authorise"),
    requestBody = Some(expectedRequestBody),
    responseStatus = Status.OK,
    responseBody = responseBody
  )

  def stubAuthoriseWithSaUtr(
    responseBody: String = responseBodyAsAgentWithSaUtr()
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
       |  "agentInformation": {},
       |  "internalId": "${internalUserId.value}",
       |  "optionalCredentials": {"providerId":"cred-id-12345","providerType":"GovernmentGateway"}
       |}
       |""".stripMargin

  def responseBodyAsAgentWithNino(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId,
    groupId: GroupId = TdAll.tdAll.groupId,
    nino: Nino = TdAll.tdAll.nino
  ): String =
    // language=JSON
    s"""
       |{
       |  "authorisedEnrolments": [],
       |  "allEnrolments": [
       |   {
       |      "identifiers": [
       |        {
       |          "key": "NINO",
       |          "value":  "${nino.value}"
       |        }
       |      ],
       |      "state": "Activated",
       |      "delegatedAuthRule": null,
       |      "key": "HMRC-PT"
       |    }
       |  ],
       |  "agentInformation": {},
       |  "internalId": "${internalUserId.value}",
       |  "optionalCredentials": {"providerId":"cred-id-12345","providerType":"GovernmentGateway"}
       |}
       |""".stripMargin

  def responseBodyAsAgentWithSaUtr(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId,
    groupId: GroupId = TdAll.tdAll.groupId,
    saUtr: SaUtr = TdAll.tdAll.saUtr
  ): String =
    // language=JSON
    s"""
       |{
       |  "authorisedEnrolments": [],
       |  "allEnrolments": [
       |   {
       |      "identifiers": [
       |        {
       |          "key": "UTR",
       |          "value":  "${saUtr.value}"
       |        }
       |      ],
       |      "state": "Activated",
       |      "delegatedAuthRule": null,
       |      "key": "IR-SA"
       |    }
       |  ],
       |  "agentInformation": {},
       |  "internalId": "${internalUserId.value}",
       |  "optionalCredentials": {"providerId":"cred-id-12345","providerType":"GovernmentGateway"}
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
      |      "affinityGroup": "Individual"
      |    }
      |  ],
      |  "retrieve": [
      |    "allEnrolments",
      |    "internalId",
      |    "optionalCredentials"
      |  ]
      |}
      |""".stripMargin
  )

}
