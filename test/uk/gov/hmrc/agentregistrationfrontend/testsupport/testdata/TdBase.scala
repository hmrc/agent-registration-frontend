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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata

import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistration.shared.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.FullName
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SafeId
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.upscan.ObjectStoreUrl
import uk.gov.hmrc.agentregistration.shared.upscan.Reference
import uk.gov.hmrc.agentregistration.shared.upscan.UploadDetails
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

trait TdBase:

  def dateString: String = "2059-11-25"
  def timeString: String = s"${dateString}T16:33:51.880"
  def localDateTime: LocalDateTime =
    // the frozen time has to be in future otherwise the applications will disappear from mongodb because of expiry index
    LocalDateTime.parse(timeString, DateTimeFormatter.ISO_DATE_TIME)
  def instant: Instant = localDateTime.toInstant(ZoneOffset.UTC)
  def newInstant: Instant = instant.plusSeconds(20) // used when a new application is created from existing one

  def utr: Utr = Utr("1234567895")
  def internalUserId: InternalUserId = InternalUserId("internal-user-id-12345")
  def groupId: GroupId = GroupId("group-id-12345")
  def nino = Nino("AB123456C")
  def safeId: SafeId = SafeId("X00000123456789")
  def dateOfBirth: LocalDate = LocalDate.of(2000, 1, 1)
  def firstName = "Test"
  def lastName = "Name"
  def fullName = FullName(firstName, lastName)
  def companyNumber = "1234567890"
  def companyName = "Test Company Name"
  def dateOfIncorporation: LocalDate = LocalDate.now().minusYears(10)
  def companyProfile = CompanyProfile(
    companyNumber = companyNumber,
    companyName = companyName,
    dateOfIncorporation = Some(dateOfIncorporation)
  )
  def postcode = "AA1 1AA"
  def validAmlsExpiryDate: LocalDate = LocalDate.now().plusMonths(6)
  def invalidAmlsExpiryDate: LocalDate = LocalDate.now().plusMonths(13)
  def amlsUploadDetailsSuccess: UploadDetails = UploadDetails(
    reference = Reference("test-file-reference"),
    status = UploadStatus.UploadedSuccessfully(
      name = "test.pdf",
      mimeType = "application/pdf",
      downloadUrl = ObjectStoreUrl(uri"http://example.com/download"),
      size = Some(12345),
      checksum = "checksum"
    )
  )
