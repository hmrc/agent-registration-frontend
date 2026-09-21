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
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker
import uk.gov.hmrc.auth.core.ConfidenceLevel

object IndividualAuthStubs {

  def stubAuthorise(
    responseBody: JsObject = responseBodyAsCleanIndividual()
  ): StubMapping = stubAuthoriseWith(responseBody)

  def stubAuthoriseWithNino(
    responseBody: JsObject = responseBodyAsIndividualWithNino()
  ): StubMapping = stubAuthoriseWith(responseBody)

  def stubAuthoriseWithSaUtr(
    responseBody: JsObject = responseBodyAsIndividualWithSaUtr()
  ): StubMapping = stubAuthoriseWith(responseBody)

  def stubAuthoriseWithNinoAndSaUtr(
    responseBody: JsObject = responseBodyAsIndividualWithNinoAndSaUtr()
  ): StubMapping = stubAuthoriseWith(responseBody)

  def stubAuthoriseWithAgentAffinity(): StubMapping = stubAuthoriseWith(responseBodyAsAgent())

  def stubUnauthorised(reason: String): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching("/auth/authorise"),
    responseStatus = Status.UNAUTHORIZED,
    responseHeaders = Seq("WWW-Authenticate" -> s"""MDTP detail="$reason"""")
  )

  def verifyAuthorise(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching("/auth/authorise"),
    count = count
  )

  def responseBodyAsCleanIndividual(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId
  ): JsObject = responseBody(internalUserId = internalUserId)

  def responseBodyAsCl50(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId
  ): JsObject = responseBodyWithCl(
    internalUserId = internalUserId,
    confidenceLevel = ConfidenceLevel.L50
  )

  def responseBodyWithCl(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId,
    confidenceLevel: ConfidenceLevel
  ): JsObject = responseBody(
    internalUserId = internalUserId,
    confidenceLevel = confidenceLevel
  )

  def responseBodyAsIndividualWithNino(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId,
    nino: Nino = TdAll.tdAll.nino
  ): JsObject = responseBody(
    internalUserId = internalUserId,
    enrolments = Seq(hmrcPtEnrolment(nino))
  )

  def responseBodyAsIndividualWithNinoInHmrcNiEnrolment(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId,
    nino: Nino = TdAll.tdAll.nino
  ): JsObject = responseBody(
    internalUserId = internalUserId,
    enrolments = Seq(hmrcNiEnrolment(nino))
  )

  /** A different Nino in each enrolment, so a test can tell which one was read. */
  def responseBodyAsIndividualWithNinoInBothEnrolments(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId,
    ninoInHmrcPt: Nino = TdAll.tdAll.nino,
    ninoInHmrcNi: Nino = Nino("AB111111A")
  ): JsObject = responseBody(
    internalUserId = internalUserId,
    enrolments = Seq(hmrcNiEnrolment(ninoInHmrcNi), hmrcPtEnrolment(ninoInHmrcPt))
  )

  def responseBodyAsIndividualWithSaUtr(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId,
    saUtr: SaUtr = TdAll.tdAll.saUtr
  ): JsObject = responseBody(
    internalUserId = internalUserId,
    enrolments = Seq(irSaEnrolment(saUtr))
  )

  def responseBodyAsIndividualWithNinoAndSaUtr(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId,
    nino: Nino = TdAll.tdAll.nino,
    saUtr: SaUtr = TdAll.tdAll.saUtr
  ): JsObject = responseBody(
    internalUserId = internalUserId,
    enrolments = Seq(hmrcPtEnrolment(nino), irSaEnrolment(saUtr))
  )

  def responseBodyAsOrganisation(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId
  ): JsObject = responseBody(
    internalUserId = internalUserId,
    affinityGroup = "Organisation"
  )

  def responseBodyWithoutAffinityGroup(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId
  ): JsObject = responseBodyAsCleanIndividual(internalUserId) - "affinityGroup"

  def responseBodyWithoutInternalId(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId
  ): JsObject = responseBodyAsCleanIndividual(internalUserId) - "internalId"

  def responseBodyWithoutCredentials(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId
  ): JsObject = responseBodyAsCleanIndividual(internalUserId) - "optionalCredentials"

  private def responseBodyAsAgent(
    internalUserId: InternalUserId = TdAll.tdAll.internalUserId
  ): JsObject = responseBody(
    internalUserId = internalUserId,
    affinityGroup = "Agent"
  )

  private def responseBody(
    internalUserId: InternalUserId,
    affinityGroup: String = "Individual",
    confidenceLevel: ConfidenceLevel = ConfidenceLevel.L250,
    enrolments: Seq[JsObject] = Seq.empty
  ): JsObject = Json.obj(
    "authorisedEnrolments" -> Json.arr(),
    "allEnrolments" -> enrolments,
    "affinityGroup" -> affinityGroup,
    "agentInformation" -> Json.obj(),
    "confidenceLevel" -> confidenceLevel.level,
    "internalId" -> internalUserId.value,
    "optionalCredentials" -> Json.obj(
      "providerId" -> "cred-id-12345",
      "providerType" -> "GovernmentGateway"
    )
  )

  private def hmrcPtEnrolment(nino: Nino): JsObject = enrolment(
    key = "HMRC-PT",
    identifierKey = "NINO",
    identifierValue = nino.value
  )

  private def hmrcNiEnrolment(nino: Nino): JsObject = enrolment(
    key = "HMRC-NI",
    identifierKey = "NINO",
    identifierValue = nino.value
  )

  private def irSaEnrolment(saUtr: SaUtr): JsObject = enrolment(
    key = "IR-SA",
    identifierKey = "UTR",
    identifierValue = saUtr.value
  )

  private def enrolment(
    key: String,
    identifierKey: String,
    identifierValue: String
  ): JsObject = Json.obj(
    "key" -> key,
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> identifierKey,
        "value" -> identifierValue
      )
    ),
    "state" -> "Activated"
  )

  private def stubAuthoriseWith(responseBody: JsObject): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching("/auth/authorise"),
    requestBody = Some(expectedRequestBody),
    responseStatus = Status.OK,
    responseBody = Json.prettyPrint(responseBody)
  )

  private val expectedRequestBody: StringValuePattern = wm.equalToJson(
    Json.prettyPrint(Json.obj(
      "authorise" -> Json.arr(
        Json.obj("authProviders" -> Json.arr("GovernmentGateway"))
      ),
      "retrieve" -> Json.arr(
        "confidenceLevel",
        "allEnrolments",
        "internalId",
        "optionalCredentials",
        "affinityGroup"
      )
    ))
  )

}
