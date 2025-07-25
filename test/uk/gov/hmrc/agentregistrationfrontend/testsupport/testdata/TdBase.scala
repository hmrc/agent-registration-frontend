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

import uk.gov.hmrc.agentregistrationfrontend.model.{Nino, Utr}

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneOffset}

trait TdBase:

  lazy val dateString: String = "2059-11-25"
  lazy val timeString: String = s"${dateString}T16:33:51.880"
  lazy val localDateTime: LocalDateTime =
    // the frozen time has to be in future otherwise the applications will disappear from mongodb because of expiry index
    LocalDateTime.parse(timeString, DateTimeFormatter.ISO_DATE_TIME)
  lazy val instant: Instant = localDateTime.toInstant(ZoneOffset.UTC)
  lazy val newInstant: Instant = instant.plusSeconds(20) // used when a new application is created from existing one

  lazy val nino: Nino = Nino("LM001014C")
  lazy val utr: Utr = Utr("1234567895")
