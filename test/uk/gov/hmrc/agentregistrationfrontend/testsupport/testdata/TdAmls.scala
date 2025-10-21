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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata

import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.upscan.ObjectStoreUrl
import uk.gov.hmrc.agentregistration.shared.upscan.Reference
import uk.gov.hmrc.agentregistration.shared.upscan.UploadDetails
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus

import java.time.LocalDate

trait TdAmls { dependencies: TdBase =>

  def amlsCode: AmlsCode = AmlsCode("HMRC")
  def amlsRegistrationNumber = AmlsRegistrationNumber("")
  def amlsExpiryDate: LocalDate = LocalDate.parse("2028-01-01")
  def amlsUploadDetails: UploadDetails = UploadDetails(
    reference = Reference("test-file-reference"),
    status = UploadStatus.UploadedSuccessfully(
      downloadUrl = ObjectStoreUrl(uri"https://bucketName.s3.eu-west-2.amazonaws.com/xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"),
      name = "evidence.pdf",
      mimeType = "application/pdf",
      size = Some(12345L),
      checksum = "md5:1B2M2Y8AsgTpgAmY7PhCfg=="
    )
  )

  object amlsDetails:

    val afterSupervisoryBodySelected: AmlsDetails = AmlsDetails(
      supervisoryBody = amlsCode,
      amlsRegistrationNumber = None,
      amlsExpiryDate = None,
      amlsEvidence = None
    )

    val afterRegistrationNumberProvided = afterSupervisoryBodySelected.copy(
      amlsRegistrationNumber = Some(amlsRegistrationNumber)
    )

    val afterAmlsExpiryDateProvided = afterRegistrationNumberProvided.copy(
      amlsExpiryDate = Some(amlsExpiryDate)
    )

    val afterUploadedEvidence = afterAmlsExpiryDateProvided.copy(
      amlsEvidence = Some(UploadStatus.InProgress)
    )
    val afterUploadFailed = afterAmlsExpiryDateProvided.copy(
      amlsEvidence = Some(UploadStatus.Failed)
    )
    val afterUploadSucceded = afterAmlsExpiryDateProvided.copy(
      amlsEvidence = Some(amlsUploadDetails)
    )

  trait AgentApplicationLlpWithAmlsDetails:

    protected def agentApplicationBaseToAddAmlsDetails: AgentApplicationLlp
    def afterSupervisoryBodySelected: AgentApplicationLlp = agentApplicationBaseToAddAmlsDetails.copy(amlsDetails =
      Some(amlsDetails.afterSupervisoryBodySelected)
    )
    def afterRegistrationNumberProvided: AgentApplicationLlp = agentApplicationBaseToAddAmlsDetails.copy(amlsDetails =
      Some(amlsDetails.afterRegistrationNumberProvided)
    )
    def afterUploadedEvidence: AgentApplicationLlp = agentApplicationBaseToAddAmlsDetails.copy(amlsDetails = Some(amlsDetails.afterUploadedEvidence))
    def afterUploadFailed: AgentApplicationLlp = agentApplicationBaseToAddAmlsDetails.copy(amlsDetails = Some(amlsDetails.afterUploadFailed))
    def afterUploadSucceded: AgentApplicationLlp = agentApplicationBaseToAddAmlsDetails.copy(amlsDetails = Some(amlsDetails.afterUploadSucceded))
    def afterAmlsExpiryDateProvided: AgentApplicationLlp = agentApplicationBaseToAddAmlsDetails.copy(amlsDetails =
      Some(amlsDetails.afterAmlsExpiryDateProvided)
    )

}
