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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication.llp

import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.FileUploadReference
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.Upload
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.UploadId
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.UploadStatus
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase

trait TdUpload { dependencies: TdBase =>

  def uploadId: UploadId = UploadId("upload-id-12345")

  def objectStoreValidHexVal: String = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

  def uploadInProgress: Upload = Upload(
    _id = dependencies.uploadId,
    internalUserId = dependencies.internalUserId,
    createdAt = dependencies.nowAsInstant,
    fileUploadReference = FileUploadReference("test-file-reference"),
    uploadStatus = UploadStatus.InProgress
  )

  def uploadUploadedSuccessfully: Upload = uploadInProgress.copy(
    uploadStatus = UploadStatus.UploadedSuccessfully(
      downloadUrl = uri"https://example.com/download-url",
      fileName = "evidence.pdf",
      mimeType = "application/pdf",
      sizeInBytes = 12345L,
      checksum = objectStoreValidHexVal
    )
  )

  def uploadFailed: Upload = uploadInProgress.copy(
    uploadStatus = UploadStatus.Failed(failureReason = "QUARANTINE", messageFromUpscan = "Suspicious file uploaded.")
  )

}
