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

import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.upscan.ObjectStoreUrl
import uk.gov.hmrc.agentregistration.shared.upscan.FileUploadReference
import uk.gov.hmrc.agentregistration.shared.upscan.Upload
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase

import java.time.LocalDate
import scala.util.chaining.scalaUtilChainingOps

trait TdSectionAmls {
  dependencies: TdBase =>

  final def amlsCodeHmrc: AmlsCode = AmlsCode("HMRC")
  def amlsCodeNonHmrc: AmlsCode = AmlsCode("ATT") /// Association of TaxationTechnicians

  def amlsRegistrationNumberHmrc = AmlsRegistrationNumber("XAML00000123456")
  def amlsRegistrationNumberNonHmrc = AmlsRegistrationNumber("NONHMRC-REF-AMLS-NUMBER-00001")

  def amlsExpiryDateValid: LocalDate = dependencies.nowPlus6mAsLocalDateTime.toLocalDate
  def amlsExpiryDateInvalid: LocalDate = dependencies.nowPlus13mAsLocalDateTime.toLocalDate

  def amlsUploadDetailsAfterUploadInProgress: Upload = Upload(
    _id = dependencies.uploadId,
    fileUploadReference = FileUploadReference("test-file-reference"),
    uploadStatus = UploadStatus.InProgress
  )

  def amlsUploadDetailsAfterUploadScannedOk: Upload = amlsUploadDetailsAfterUploadInProgress.copy(
    uploadStatus = dependencies.successfulUploadStatus.modify(_.objectStoreLocation).setTo(None)
  )

  def amlsUploadDetailsAfterUploadSucceeded: Upload = amlsUploadDetailsAfterUploadInProgress.copy(
    uploadStatus = dependencies.successfulUploadStatus
  )

  private def amlsUploadDetailsAfterUploadFailedScanning: Upload = amlsUploadDetailsAfterUploadInProgress.copy(
    uploadStatus = UploadStatus.Failed
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
          ).tap(x => require(x.isComplete, "when HMRC - no evidence or expiry date is required to be considered complete"))

        def afterSupervisoryBodySelected: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(amlsDetailsHelper.afterSupervisoryBodySelected))

        def afterRegistrationNumberProvided: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails =
          Some(amlsDetailsHelper.afterRegistrationNumberProvided)
        )

        def complete: AgentApplicationLlp = afterRegistrationNumberProvided.tap(x => require(x.amlsDetails.exists(_.isComplete), "sanity check"))

      object whenSupervisorBodyIsNonHmrc:

        def amlsCode: AmlsCode = amlsCodeNonHmrc
        def amlsRegistrationNumber: AmlsRegistrationNumber = amlsRegistrationNumberNonHmrc

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
            amlsExpiryDate = Some(amlsExpiryDateValid)
          )
          def afterUploadInitiated = afterAmlsExpiryDateProvided.copy(
            amlsEvidence = Some(amlsUploadDetailsAfterUploadInProgress)
          )
          def afterUploadFailed = afterAmlsExpiryDateProvided.copy(
            amlsEvidence = Some(amlsUploadDetailsAfterUploadFailedScanning)
          )
          def afterUploadScannedOk = afterAmlsExpiryDateProvided.copy(
            amlsEvidence = Some(amlsUploadDetailsAfterUploadScannedOk)
          )
          def afterUploadSucceeded = afterAmlsExpiryDateProvided.copy(
            amlsEvidence = Some(amlsUploadDetailsAfterUploadSucceeded)
          )

        def afterSupervisoryBodySelected: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(amlsDetailsHelper.afterSupervisoryBodySelected))

        def afterRegistrationNumberProvided: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails =
          Some(amlsDetailsHelper.afterRegistrationNumberProvided)
        )

        def afterAmlsExpiryDateProvided: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(amlsDetailsHelper.afterAmlsExpiryDateProvided))

        def afterUploadInitiated: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(amlsDetailsHelper.afterUploadInitiated))

        def afterUploadFailed: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(amlsDetailsHelper.afterUploadFailed))

        def afterUploadScannedOk: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(amlsDetailsHelper.afterUploadScannedOk))
        def afterUploadSucceeded: AgentApplicationLlp = baseForSectionAmls.copy(amlsDetails = Some(amlsDetailsHelper.afterUploadSucceeded))

        def complete: AgentApplicationLlp = afterUploadSucceeded.tap(x => require(x.amlsDetails.exists(_.isComplete), "sanity check"))

}
