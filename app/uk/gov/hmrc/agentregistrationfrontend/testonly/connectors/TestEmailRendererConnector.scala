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

import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.Connector
import uk.gov.hmrc.http.client.HttpClientV2

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class TestEmailRendererConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(using
  ec: ExecutionContext
)
extends Connector:

  def renderEmailHtml(
    templateId: String,
    parameters: Map[String, String]
  )(using RequestHeader): Future[String] =
    val url: URL = url"$baseUrl/templates/$templateId"
    httpClient
      .post(url)
      .withBody(Json.obj("parameters" -> parameters))
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if status === Status.OK =>
            val htmlBase64: String = (response.json \ "html").as[String]
            new String(Base64.getDecoder.decode(htmlBase64), StandardCharsets.UTF_8)
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to render email template $templateId")

  private val baseUrl: String = appConfig.hmrcEmailRendererBaseUrl
