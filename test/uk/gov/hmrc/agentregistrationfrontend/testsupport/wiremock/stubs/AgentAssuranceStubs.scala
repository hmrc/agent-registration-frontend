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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.client.WireMock.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object AgentAssuranceStubs {

  def stubIsRefusedToDealWith(
    saUtr: String,
    isRefused: Boolean
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlEqualTo(s"/agent-assurance/refusal-to-deal-with/utr/$saUtr"),
    responseStatus =
      if (isRefused)
        403
      else
        200,
    responseBody = ""
  )

  def verifyIsRefusedToDealWith(
    utr: String,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlEqualTo(s"/agent-assurance/refusal-to-deal-with/utr/$utr"),
    count = count
  )

}
