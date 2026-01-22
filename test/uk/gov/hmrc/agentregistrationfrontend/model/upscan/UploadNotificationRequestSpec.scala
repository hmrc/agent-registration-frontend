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

package uk.gov.hmrc.agentregistrationfrontend.model.upscan

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.upscan.FileUploadReference
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.upscan.UploadEventDetails
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.upscan.UploadNotificationRequest
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec

import java.time.Instant

class UploadNotificationRequestSpec
extends UnitSpec:

  "read json" in:

    val json: JsValue = Json.parse:
      // language=JSON
      """{
        |  "reference" : "c7f0eca8-1ae8-4f13-8e8b-f4651c107982",
        |  "downloadUrl" : "http://localhost:9570/upscan/download/90216d51-08c0-4d69-b8d0-780349e22882",
        |  "fileStatus" : "READY",
        |  "uploadDetails" : {
        |    "size" : 3371292,
        |    "fileMimeType" : "application/pdf",
        |    "fileName" : "AMLS Confirmation.pdf",
        |    "checksum" : "27281179c4cfeed617ae568ff271423924207b848dde546419becd4be227a9f1",
        |    "uploadTimestamp" : "2025-12-30T15:07:30.609774735Z"
        |  }
        |}""".stripMargin

    json.as[UploadNotificationRequest] shouldBe UploadNotificationRequest.Succeeded(
      reference = FileUploadReference("c7f0eca8-1ae8-4f13-8e8b-f4651c107982"),
      downloadUrl = uri"http://localhost:9570/upscan/download/90216d51-08c0-4d69-b8d0-780349e22882",
      uploadDetails = UploadEventDetails(
        uploadTimestamp = Instant.parse("2025-12-30T15:07:30.609774735Z"),
        checksum = "27281179c4cfeed617ae568ff271423924207b848dde546419becd4be227a9f1",
        fileMimeType = "application/pdf",
        fileName = "AMLS Confirmation.pdf",
        size = 3371292L
      )
    )
