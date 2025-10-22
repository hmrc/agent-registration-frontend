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
import uk.gov.hmrc.agentregistration.shared.upscan.ObjectStoreUrl
import uk.gov.hmrc.agentregistration.shared.upscan.Reference
import uk.gov.hmrc.agentregistration.shared.upscan.UploadDetails
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase

import java.time.LocalDate

trait TdSectionAmls {
  dependencies: TdBase =>

  def amlsCode: AmlsCode = AmlsCode("HMRC")
  def amlsRegistrationNumber = AmlsRegistrationNumber("XAML00000123456")
  def amlsExpiryDate: LocalDate = LocalDate.parse("2028-01-01")

  def amlsUploadDetailsAfterUploadInProgress: UploadDetails = UploadDetails(
    reference = Reference("test-file-reference"),
    status = UploadStatus.InProgress
  )

  def amlsUploadDetailsAfterUploadSucceeded: UploadDetails = amlsUploadDetailsAfterUploadInProgress.copy(
    status = UploadStatus.UploadedSuccessfully(
      downloadUrl = ObjectStoreUrl(uri"https://bucketName.s3.eu-west-2.amazonaws.com/xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"),
      name = "evidence.pdf",
      mimeType = "application/pdf",
      size = Some(12345L),
      checksum = "md5:1B2M2Y8AsgTpgAmY7PhCfg=="
    )
  )

  def amlsUploadDetailsAfterUploadFailed: UploadDetails = amlsUploadDetailsAfterUploadInProgress.copy(
    status = UploadStatus.Failed(
      failureReason = "Invalid file type"
    )
  )

  class AgentApplicationLlpWithSectionAmls(baseForSectionAmls: AgentApplicationLlp):

    object sectionAmls:

      def afterSupervisoryBodySelected: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(AmlsDetailsHelper.afterSupervisoryBodySelected))
      def afterRegistrationNumberProvided: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(AmlsDetailsHelper.afterRegistrationNumberProvided))
      def afterAmlsExpiryDateProvided: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(AmlsDetailsHelper.afterAmlsExpiryDateProvided))
      def afterUploadedEvidence: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(AmlsDetailsHelper.afterUploadedEvidence))
      def afterUploadFailed: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(AmlsDetailsHelper.afterUploadFailed))
      def afterUploadSucceded: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(AmlsDetailsHelper.afterUploadSucceded))

  private object AmlsDetailsHelper:

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
      amlsEvidence = Some(amlsUploadDetailsAfterUploadInProgress)
    )
    val afterUploadFailed = afterAmlsExpiryDateProvided.copy(
      amlsEvidence = Some(amlsUploadDetailsAfterUploadFailed)
    )
    val afterUploadSucceded = afterAmlsExpiryDateProvided.copy(
      amlsEvidence = Some(amlsUploadDetailsAfterUploadSucceeded)
    )

}
