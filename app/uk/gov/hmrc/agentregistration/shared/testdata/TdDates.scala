/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.shared.testdata

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

object TdDates:

  private val dateString: String = "2059-11-25"
  private val timeString: String = s"${dateString}T16:33:51.880"
  private val nowAsLocalDateTime: LocalDateTime =
    // the frozen time has to be in future otherwise the applications will disappear from mongodb because of expiry index
    LocalDateTime.parse(timeString, DateTimeFormatter.ISO_DATE_TIME)

  val instant: Instant = nowAsLocalDateTime.toInstant(ZoneOffset.UTC) // when the application is created
  val instant1DayLater: Instant = instant.plus(1, ChronoUnit.DAYS)
  val instant2DaysLater: Instant = instant.plus(2, ChronoUnit.DAYS)
  val instant3DaysLater: Instant = instant.plus(3, ChronoUnit.DAYS)
  val instant4DaysLater: Instant = instant.plus(4, ChronoUnit.DAYS)
  val instant20DaysLater: Instant = instant.plus(20, ChronoUnit.DAYS) // individual created
  val instant21DaysLater: Instant = instant.plus(20, ChronoUnit.DAYS) // individual confirmed
  val instant72DaysLater: Instant = instant.plus(72, ChronoUnit.DAYS) // declaration submitted
  val instant73DaysLater: Instant = instant.plus(73, ChronoUnit.DAYS)
