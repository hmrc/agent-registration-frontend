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

import com.typesafe.config.ConfigMemorySize
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.mvc.RequestHeader
import sttp.model.Uri
import uk.gov.hmrc.agentregistration.shared.upscan.FileUploadReference
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.UpscanInitiateConnector.*
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.hc
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class UpscanInitiateConnector @Inject() (
  appConfig: AppConfig,
  httpClientV2: HttpClientV2
)(using ExecutionContext):

  private val baseUrl: String = appConfig.upscanInitiateBaseUrl

  /** @see
    *   https://github.com/hmrc/upscan-initiate?tab=readme-ov-file#post-upscanv2initiate
    */
  def initiate(
    redirectOnSuccessUrl: Uri,
    redirectOnErrorUrl: Uri,
    maxFileSize: ConfigMemorySize
  )(using RequestHeader): Future[UpscanInitiateResponse] = httpClientV2
    .post(url"$baseUrl/upscan/v2/initiate")
    .withBody(Json.toJson(UpscanInitiateRequest(
      callbackUrl = appConfig.Upscan.callbackUrl,
      successRedirect = Some(redirectOnSuccessUrl.toString),
      errorRedirect = Some(redirectOnErrorUrl.toString),
      maximumFileSize = Some(maxFileSize.toBytes)
    )))
    .execute[UpscanInitiateResponse]

object UpscanInitiateConnector:

  final case class UpscanInitiateRequest(
    callbackUrl: String,
    successRedirect: Option[String] = None,
    errorRedirect: Option[String] = None,
    minimumFileSize: Option[Long] = None,
    maximumFileSize: Option[Long] = None
  )

  object UpscanInitiateRequest:
    given OFormat[UpscanInitiateRequest] = Json.format[UpscanInitiateRequest]

  final case class UpscanInitiateResponse(
    reference: FileUploadReference,
    uploadRequest: UploadRequest
  )

  object UpscanInitiateResponse:
    given Format[UpscanInitiateResponse] = Json.format[UpscanInitiateResponse]

  final case class UploadRequest(
    href: String,
    fields: Map[String, String]
  )

  object UploadRequest:
    given Format[UploadRequest] = Json.format[UploadRequest]
