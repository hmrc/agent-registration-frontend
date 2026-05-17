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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import uk.gov.hmrc.agentregistration.shared.Arn
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object TaxEnrolmentsStubs:

  def stubAddKnownFacts(
    arn: Arn
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.PUT,
    urlPattern = wm.urlEqualTo(s"/tax-enrolments/enrolments/HMRC-AS-AGENT~AgentReferenceNumber~${arn.value}"),
    responseStatus = Status.NO_CONTENT,
    responseBody = ""
  )

  def verifyAddKnownFacts(
    arn: Arn,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.PUT,
    urlPattern = wm.urlEqualTo(s"/tax-enrolments/enrolments/HMRC-AS-AGENT~AgentReferenceNumber~${arn.value}"),
    count = count
  )

  def stubEnrol(
    arn: Arn
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching(s"/tax-enrolments/groups/[^/]+/enrolments/HMRC-AS-AGENT~AgentReferenceNumber~${arn.value}"),
    responseStatus = Status.NO_CONTENT,
    responseBody = ""
  )

  def verifyEnrol(
    arn: Arn,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlMatching(s"/tax-enrolments/groups/[^/]+/enrolments/HMRC-AS-AGENT~AgentReferenceNumber~${arn.value}"),
    count = count
  )
