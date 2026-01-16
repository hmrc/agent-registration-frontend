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

package uk.gov.hmrc.agentregistrationfrontend.testonly.connectors

import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.Connector
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.BusinessPartnerRecord
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

/** Connector to agents external stubs service used in test environments only
  */
@Singleton
class AgentsExternalStubsConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(using
  ExecutionContext
)
extends Connector:

  def storeBusinessPartnerRecord(bpr: BusinessPartnerRecord)(using
    request: RequestHeader
  ): Future[Unit] =
    val url: URL = url"$baseUrl/records/business-partner-record"
    httpClient
      .post(url)
      .withBody(Json.toJson(bpr))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.CREATED => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure("Failed to store business partner record")

  private val baseUrl: String = appConfig.agentsExternalStubsBaseUrl + "/agents-external-stubs"
