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

package uk.gov.hmrc.agentregistrationfrontend.testonly.connectors

import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.testonly.FindRiskingResultsFilesRequest
import uk.gov.hmrc.agentregistration.shared.testonly.FindRiskingResultsFilesResponse
import uk.gov.hmrc.agentregistration.shared.testonly.RiskingResultsFileName
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.TestRiskingStubs

class TestRiskingConnectorSpec
extends ISpec:

  "findRiskingResultsFiles should send the request and read the files the risking service returns" in:
    TestRiskingStubs.stubFindRiskingResultsFiles(findRiskingResultsFilesRequest, findRiskingResultsFilesResponse)

    connector.findRiskingResultsFiles(findRiskingResultsFilesRequest)(using FakeRequest()).futureValue shouldBe findRiskingResultsFilesResponse
    TestRiskingStubs.verifyFindRiskingResultsFiles(findRiskingResultsFilesRequest)

  private lazy val connector: TestRiskingConnector = app.injector.instanceOf[TestRiskingConnector]
  private lazy val findRiskingResultsFilesRequest: FindRiskingResultsFilesRequest = FindRiskingResultsFilesRequest(
    applicationReferences = List(ApplicationReference("XARN0000001")),
    personReferences = List(PersonReference("PERS0000001"), PersonReference("PERS0000002"))
  )

  private lazy val findRiskingResultsFilesResponse: FindRiskingResultsFilesResponse = FindRiskingResultsFilesResponse(
    fileNameByApplicationReference = Map(ApplicationReference("XARN0000001") -> RiskingResultsFileName("test-only-combined-20261015T110000000Z")),
    fileNameByPersonReference = Map(PersonReference("PERS0000001") -> RiskingResultsFileName("test-only-combined-20261015T110000000Z"))
  )
