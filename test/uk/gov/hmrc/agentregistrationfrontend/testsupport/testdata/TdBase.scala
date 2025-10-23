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
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.upscan.ObjectStoreUrl
import uk.gov.hmrc.agentregistration.shared.upscan.Reference
import uk.gov.hmrc.agentregistration.shared.upscan.UploadDetails
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

trait TdBase:

  final val zoneOffset: ZoneOffset = ZoneOffset.UTC
  final val zoneId: ZoneId = ZoneId.of("UTC")

  def dateString: String = "2059-11-25"
  def timeString: String = s"${dateString}T16:33:51.880"

  def nowAsLocalDateTime: LocalDateTime =
    // the frozen time has to be in future otherwise the applications will disappear from mongodb because of expiry index
    LocalDateTime.parse(timeString, DateTimeFormatter.ISO_DATE_TIME)

  def nowPlus6mAsLocalDateTime: LocalDateTime = nowAsLocalDateTime.plus(java.time.Period.ofMonths(6))
  def nowPlus13mAsLocalDateTime: LocalDateTime = nowAsLocalDateTime.plus(java.time.Period.ofMonths(13))
  def newPlus20sAsLocalDateTime: LocalDateTime = nowAsLocalDateTime.plusSeconds(20)

  def nowAsInstant: Instant = nowAsLocalDateTime.toInstant(ZoneOffset.UTC)

  final val clock: Clock = Clock.fixed(nowAsInstant, zoneId)

  def saUtr: SaUtr = SaUtr("1234567895")
  def internalUserId: InternalUserId = InternalUserId("internal-user-id-12345")
  def groupId: GroupId = GroupId("group-id-12345")
  def nino = Nino("AB123456C")
  def safeId: SafeId = SafeId("X00000123456789")
  def dateOfBirth: LocalDate = LocalDate.of(2000, 1, 1)

  def telephoneNumber: TelephoneNumber = TelephoneNumber("(+44) 10794554342")
  def crn: Crn = Crn("1234567890")
  def companyName = "Test Company Name"
  def dateOfIncorporation: LocalDate = LocalDate.now().minusYears(10)
  def companyProfile: CompanyProfile = CompanyProfile(
    companyNumber = crn,
    companyName = companyName,
    dateOfIncorporation = Some(dateOfIncorporation)
  )
  def postcode: String = "AA1 1AA"
