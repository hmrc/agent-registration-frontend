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
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistrationfrontend.shared.model.emailverification.VerifyEmailRequest
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object EmailVerificationStubs {

  private def stubEmailStatus(
    credId: String,
    emailAddress: EmailAddress,
    verified: Boolean,
    locked: Boolean
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/email-verification/verification-status/$credId"),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "emails" -> Json.arr(
          Json.obj(
            "emailAddress" -> s"${emailAddress.value}",
            "verified" -> verified,
            "locked" -> locked
          )
        )
      ).toString
  )

  def stubEmailStatusUnverified(
    credId: String,
    emailAddress: EmailAddress
  ): StubMapping = stubEmailStatus(
    credId,
    emailAddress,
    verified = false,
    locked = false
  )

  def stubEmailStatusVerified(
    credId: String,
    emailAddress: EmailAddress
  ): StubMapping = stubEmailStatus(
    credId,
    emailAddress,
    verified = true,
    locked = false
  )

  def stubEmailStatusLocked(
    credId: String,
    emailAddress: EmailAddress
  ): StubMapping = stubEmailStatus(
    credId,
    emailAddress,
    verified = false,
    locked = true
  )

  def stubEmailYetToBeVerified(
    credId: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/email-verification/verification-status/$credId"),
    responseStatus = 404,
    responseBody = Json.obj("error" -> s"no verified or locked emails found for cred ID: $credId").toString
  )

  def stubVerificationRequest(verifyEmailRequest: VerifyEmailRequest): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlMatching(s"/email-verification/verify-email"),
    responseStatus = 201,
    requestBody = Some(wm.equalToJson(
      Json.toJson(verifyEmailRequest).toString
    )),
    responseBody = Json.obj("redirectUri" -> "/response-url").toString
  )

  def verifyEvStatusRequest(
    credId: String,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = urlMatching(s"/email-verification/verification-status/$credId"),
    count = count
  )

  def verifyEvRequest(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = urlMatching(s"/email-verification/verify-email"),
    count = count
  )

}
