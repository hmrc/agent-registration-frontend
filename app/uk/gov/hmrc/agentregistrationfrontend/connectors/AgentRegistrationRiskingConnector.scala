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

package uk.gov.hmrc.agentregistrationfrontend.connectors

import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitForRiskingRequest
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

/** Connector to the companion backend microservice
  */
@Singleton
class AgentRegistrationRiskingConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(using
  ExecutionContext
)
extends Connector:

  def submitForRisking(submitForRiskingRequest: SubmitForRiskingRequest)(using RequestHeader): Future[Unit] =
    val url: URL = url"$baseUrl/submit-for-risking/${submitForRiskingRequest.agentApplication.agentApplicationId}"
    httpClient
      .post(url)
      .withBody(Json.toJson(submitForRiskingRequest))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.ACCEPTED => ()
          case other =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = other,
              response = response,
              info = "submit for risking problem"
            )
      .andLogOnFailure(s"Failed to submit agent application for risking: ${submitForRiskingRequest.agentApplication.agentApplicationId}")

  private val baseUrl: String = appConfig.agentRegistrationRiskingBaseUrl + "/agent-registration-risking"
