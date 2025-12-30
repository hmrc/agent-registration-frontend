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

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll
import uk.gov.hmrc.http.StringContextOps

class UploadStatusSpec
extends UnitSpec:

  "De/Serialize UploadStatus" in:

    val uploadedSuccessfully: UploadStatus = UploadStatus.UploadedSuccessfully(
      fileName = "test.pdf",
      mimeType = "application/pdf",
      downloadUrl = url"http://localhost:9000/test.pdf",
      size = Some(1000),
      checksum = TdAll.tdAll.objectStoreValidHexVal
    )

    val uploadedSuccessfullyJson: JsValue = Json.parse(
      // language=JSON
      s"""
         |{
         |  "name": "test.pdf",
         |  "mimeType": "application/pdf",
         |  "downloadUrl": "http://localhost:9000/test.pdf",
         |  "size": 1000,
         |  "checksum": "${TdAll.tdAll.objectStoreValidHexVal}",
         |  "type": "UploadedSuccessfully"
         |}
         |""".stripMargin
    )

    val uploadFailed: UploadStatus = UploadStatus.Failed
    val uploadFailedJson: JsValue = Json.parse(
      // language=JSON
      """
        |{
        |  "type": "Failed"
        |}
        |""".stripMargin
    )

    val uploadInProgressJson: JsValue = Json.parse(
      // language=JSON
      """{"type":"InProgress"}"""
    )

    Json.toJson(uploadedSuccessfully) shouldBe uploadedSuccessfullyJson withClue "serialize"
    uploadedSuccessfullyJson.as[UploadStatus] shouldBe uploadedSuccessfully withClue "deserialize"

    Json.toJson(uploadFailed) shouldBe uploadFailedJson withClue "serialize"
    uploadFailedJson.as[UploadStatus] shouldBe uploadFailed withClue "deserialize"

    Json.toJson[UploadStatus](UploadStatus.InProgress) shouldBe uploadInProgressJson withClue "serialize"
    uploadInProgressJson.as[UploadStatus] shouldBe UploadStatus.InProgress withClue "deserialize"
