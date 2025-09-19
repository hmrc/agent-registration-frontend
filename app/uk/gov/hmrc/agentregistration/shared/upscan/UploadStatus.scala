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

package uk.gov.hmrc.agentregistration.shared.upscan

import play.api.libs.json.*

sealed trait UploadStatus

object UploadStatus:

  case object InProgress
  extends UploadStatus

  final case class Failed(failureReason: String)
  extends UploadStatus

  final case class UploadedSuccessfully(
    name: String,
    mimeType: String,
    downloadUrl: ObjectStoreUrl,
    size: Option[Long],
    checksum: String
  )
  extends UploadStatus

  given OFormat[UploadStatus] =
    given OFormat[InProgress.type] = Json.format[InProgress.type]
    given OFormat[Failed] = Json.format[Failed]
    given OFormat[UploadedSuccessfully] = Json.format[UploadedSuccessfully]

    Json.format[UploadStatus]
