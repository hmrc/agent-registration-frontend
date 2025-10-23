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
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.upscan.ObjectStoreUrl
import uk.gov.hmrc.agentregistration.shared.upscan.Reference
import uk.gov.hmrc.agentregistration.shared.upscan.UploadDetails
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus
import uk.gov.hmrc.agentregistrationfrontend.testsupport.RichMatchers.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase

import java.time.LocalDate
import scala.util.chaining.scalaUtilChainingOps

trait TdSectionAmls {
  dependencies: TdBase =>

  final def amlsCodeHmrc: AmlsCode = AmlsCode("HMRC")
  def amlsCodeNonHmrc: AmlsCode = AmlsCode("ATT") /// Association of TaxationTechnicians

  def amlsRegistrationNumberHmrc = AmlsRegistrationNumber("XAML00000123456")
  def amlsRegistrationNumberNonHmrc = AmlsRegistrationNumber("NONHMRC_REF_AMLS_NUMBER_00001")

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

    /** when the supervisory body is HMRC, the registration number has a different format to non-HMRC bodies and no evidence or expiry date is required to be
      * considered complete
      */
    object sectionAmls:

      object whenSupervisorBodyIsHmrc:

        def amlsCode: AmlsCode = amlsCodeHmrc
        def amlsRegistrationNumber: AmlsRegistrationNumber = amlsRegistrationNumberHmrc

        private object amlsDetailsHelper:

          def afterSupervisoryBodySelected: AmlsDetails = AmlsDetails(
            supervisoryBody = amlsCode,
            amlsRegistrationNumber = None,
            amlsExpiryDate = None,
            amlsEvidence = None
          )

          def afterRegistrationNumberProvided: AmlsDetails = afterSupervisoryBodySelected.copy(
            amlsRegistrationNumber = Some(amlsRegistrationNumber)
          ).tap(_.isComplete shouldBe true withClue "when HMRC - no evidence or expiry date is required to be considered complete")

        def afterSupervisoryBodySelected: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(amlsDetailsHelper.afterSupervisoryBodySelected))

        def afterRegistrationNumberProvided: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails =
          Some(amlsDetailsHelper.afterRegistrationNumberProvided)
        )

        def complete: AgentApplicationLlp = afterRegistrationNumberProvided.tap(_.amlsDetails.value.isComplete shouldBe true withClue "sanity check")

      object whenSupervisorBodyIsNonHmrc:

        def amlsCode: AmlsCode = amlsCodeNonHmrc
        def amlsRegistrationNumber: AmlsRegistrationNumber = amlsRegistrationNumberHmrc

        private object amlsDetailsHelper:

          def afterSupervisoryBodySelected: AmlsDetails = AmlsDetails(
            supervisoryBody = amlsCode,
            amlsRegistrationNumber = None,
            amlsExpiryDate = None,
            amlsEvidence = None
          )

          def afterRegistrationNumberProvided = afterSupervisoryBodySelected.copy(
            amlsRegistrationNumber = Some(amlsRegistrationNumber)
          )

          def afterAmlsExpiryDateProvided = afterRegistrationNumberProvided.copy(
            amlsExpiryDate = Some(amlsExpiryDate)
          )
          def afterUploadedEvidence = afterAmlsExpiryDateProvided.copy(
            amlsEvidence = Some(amlsUploadDetailsAfterUploadInProgress)
          )
          def afterUploadFailed = afterAmlsExpiryDateProvided.copy(
            amlsEvidence = Some(amlsUploadDetailsAfterUploadFailed)
          )
          def afterUploadSucceded = afterAmlsExpiryDateProvided.copy(
            amlsEvidence = Some(amlsUploadDetailsAfterUploadSucceeded)
          )

        def afterSupervisoryBodySelected: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(amlsDetailsHelper.afterSupervisoryBodySelected))

        def afterRegistrationNumberProvided: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails =
          Some(amlsDetailsHelper.afterRegistrationNumberProvided)
        )

        def afterAmlsExpiryDateProvided: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(amlsDetailsHelper.afterAmlsExpiryDateProvided))

        def afterUploadedEvidence: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(amlsDetailsHelper.afterUploadedEvidence))

        def afterUploadFailed: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(amlsDetailsHelper.afterUploadFailed))

        def afterUploadSucceded: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(amlsDetailsHelper.afterUploadSucceded))

        def complete: AgentApplicationLlp = afterUploadSucceded.tap(_.amlsDetails.value.isComplete shouldBe true withClue "sanity check")

}
