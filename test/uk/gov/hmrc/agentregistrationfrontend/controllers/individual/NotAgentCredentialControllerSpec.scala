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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import com.github.tomakehurst.wiremock.client.WireMock as wm
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import java.net.URLEncoder

class NotAgentCredentialControllerSpec
extends ControllerSpec:

  private val continueUrl = s"$thisFrontendBaseUrl/agent-registration/provide-details/start/${tdAll.linkId.value}"
  private val encodedContinueUrl = URLEncoder.encode(continueUrl, "UTF-8")
  private val path = s"/agent-registration/apply/not-agent-credential?continueUrl=$encodedContinueUrl"

  "NotAgentCredentialController should have the correct routes" in:
    AppRoutes.providedetails.NotAgentCredentialController.show(Some(RedirectUrl(continueUrl))) shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should return 200 for Agent affinity and render the not agent credential page without fetching an application" in:
    AuthStubs.stubAuthoriseAgentAffinityOnly()

    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe
      "You need to sign in with personal tax account details - Apply for an agent services account - GOV.UK"
    AuthStubs.verifyAuthorise()
    StubMaker.verify(
      httpMethod = StubMaker.HttpMethod.GET,
      urlPattern = wm.urlMatching("/agent-registration/.*"),
      count = 0
    )

  s"GET $path should return unauthorised when the user is not Agent affinity" in:
    AuthStubs.stubUnauthorized(reason = "UnsupportedAffinityGroup")

    val response: WSResponse = get(path)

    response.status shouldBe Status.UNAUTHORIZED
