/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.connectors

import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.shared.model.emailverification.VerificationStatusResponse
import uk.gov.hmrc.agentregistrationfrontend.shared.model.emailverification.VerifyEmailRequest
import uk.gov.hmrc.agentregistrationfrontend.shared.model.emailverification.VerifyEmailResponse
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class EmailVerificationConnector @Inject() (
  http: HttpClientV2,
  appConfig: AppConfig
)(using ExecutionContext)
extends Connector:

  def verifyEmail(verifyEmailRequest: VerifyEmailRequest)(using
    RequestHeader
  ): Future[VerifyEmailResponse] =
    val url: URL = url"${appConfig.emailVerificationBaseUrl}/email-verification/verify-email"
    http
      .post(url)
      .withBody(Json.toJson(verifyEmailRequest))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if is2xx(status) => response.json.as[VerifyEmailResponse]
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to verify email")

  def checkEmailVerificationStatus(credId: String)(using
    RequestHeader
  ): Future[VerificationStatusResponse] =
    val url: URL = url"${appConfig.emailVerificationBaseUrl}/email-verification/verification-status/$credId"
    http
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case 200 => response.json.as[VerificationStatusResponse]
          case 404 => VerificationStatusResponse(List.empty)
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to check email verification status for credId")
