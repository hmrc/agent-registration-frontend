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

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object AuditStubs:

  val stubUrl = "/write/audit.*"

  def stubAudit(): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlMatching(stubUrl),
    responseStatus = 204
  )

  def verifyAuditEvent(
    auditType: String,
    journeyType: Option[String] = None,
    count: Int = 1
  ): Unit = {
    val requestBody =
      journeyType match
        case Some(jt) => matchingJsonPath("$.auditType", equalTo(auditType)).and(matchingJsonPath("*.journeyType", equalTo(jt)))
        case None => matchingJsonPath("$.auditType", equalTo(auditType))
    StubMaker.verify(
      httpMethod = StubMaker.HttpMethod.POST,
      urlPattern = urlMatching(stubUrl),
      count = count,
      requestBody = Some(requestBody)
    )
  }
