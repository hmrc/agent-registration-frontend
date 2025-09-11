/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistration.shared.upscan.Reference
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.PreparedUpload
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.UpscanInitiateRequest
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.UpscanInitiateResponse
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class UpscanConnector @Inject() (
  appConfig: AppConfig,
  httpClientV2: HttpClientV2
)(implicit ec: ExecutionContext) {

  private val upscanInitiateHost: String = appConfig.upscanInitiateHost
  private[connectors] val upscanInitiatePath: String = "/upscan/v2/initiate"
  private val upscanInitiateUrl: String = upscanInitiateHost + upscanInitiatePath
  private val headers = Map(
    HeaderNames.CONTENT_TYPE -> "application/json"
  )
  def initiate(
    redirectOnSuccess: Option[String],
    redirectOnError: Option[String],
    maxFileSize: Int
  )(using HeaderCarrier): Future[UpscanInitiateResponse] =
    val request = UpscanInitiateRequest(
      callbackUrl = appConfig.upscanCallbackEndpoint,
      successRedirect = redirectOnSuccess,
      errorRedirect = redirectOnError,
      maximumFileSize = Some(maxFileSize)
    )

    for
      response <- httpClientV2.post(url"$upscanInitiateUrl")
        .withBody(Json.toJson(request))
        .setHeader(headers.toSeq*)
        .execute[PreparedUpload]
      fileReference = Reference(response.reference.value)
      postTarget = response.uploadRequest.href
      formFields = response.uploadRequest.fields
    yield UpscanInitiateResponse(
      fileReference,
      postTarget,
      formFields
    )

}
