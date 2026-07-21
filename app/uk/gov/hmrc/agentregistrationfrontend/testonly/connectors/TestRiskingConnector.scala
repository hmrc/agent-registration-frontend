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

import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.Connector
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class TestRiskingConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(using
  ec: ExecutionContext
)
extends Connector:

  def deleteAllApplications()(using RequestHeader): Future[Unit] =
    val url: URL = url"$baseUrl/applications"
    httpClient
      .delete(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.NO_CONTENT => ()
          case other =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "DELETE",
              url = url,
              status = other,
              response = response
            )
      .andLogOnFailure("Failed to delete all risking Agent Applications")

  def uploadRiskingResultsFile(
    filename: String,
    body: JsValue
  )(using RequestHeader): Future[Unit] =
    val url: URL = url"$baseUrl/risking-results-file/$filename"
    httpClient
      .post(url)
      .withBody(body)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if is2xx(status) => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to upload risking results file: $filename")

  private val baseUrl: String = appConfig.agentRegistrationRiskingBaseUrl + "/agent-registration-risking/test-only"
