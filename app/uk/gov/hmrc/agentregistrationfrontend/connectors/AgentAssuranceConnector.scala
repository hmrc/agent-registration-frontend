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

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.EntityCheckResult
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.given
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AgentAssuranceConnector @Inject() (
  http: HttpClientV2,
  appConfig: AppConfig
)(implicit val ec: ExecutionContext)
extends RequestAwareLogging:

  def isRefusedToDealWith(utr: Utr)(using
    rh: RequestHeader
  ): Future[EntityCheckResult] =
    val url = url"${appConfig.agentAssuranceBaseUrl}/agent-assurance/refusal-to-deal-with/utr/${utr.value}"
    http
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case 403 => EntityCheckResult.Fail
          case 200 => EntityCheckResult.Pass
          case status =>
            logger.error(s"refusal-to-deal-with check error for ${utr.value}; HTTP status: $status")
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
