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
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistrationfrontend.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object EnrolmentStoreStubs {

  def stubQueryEnrolmentsAllocatedToGroup(
    groupId: GroupId,
    enrolment: EnrolmentStoreProxyConnector.Enrolment
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlMatching(s"/enrolment-store-proxy/enrolment-store/groups/${groupId.value}/enrolments"),
    responseStatus = Status.OK,
    responseBody = Json.prettyPrint(Json.arr(Json.obj(
      "service" -> enrolment.service,
      "state" -> enrolment.state
    )))
  )

  def stubQueryEnrolmentsAllocatedToGroupNoContent(
    groupId: GroupId
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlMatching(s"/enrolment-store-proxy/enrolment-store/groups/${groupId.value}/enrolments"),
    responseStatus = Status.NO_CONTENT,
    responseBody = ""
  )

  def verifyQueryEnrolmentsAllocatedToGroup(
    groupId: GroupId
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlMatching(s"/enrolment-store-proxy/enrolment-store/groups/${groupId.value}/enrolments")
  )

}
