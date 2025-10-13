/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.shared.contactdetails

import play.api.libs.json.*
import play.api.libs.functional.syntax.*

final case class CompaniesHouseOfficer(
  name: String,
  dateOfBirth: Option[CompaniesHouseDateOfBirth]
):
  override def toString: String = s"${name}|${dateOfBirth.map(dob => s"${dob.day.getOrElse("")}/${dob.month}/${dob.year}").getOrElse("")}"

object CompaniesHouseOfficer:

  implicit val reads: Reads[CompaniesHouseOfficer] =
    (
      (__ \ "name").read[String] and
        (__ \ "date_of_birth").readNullable[CompaniesHouseDateOfBirth]
    )(CompaniesHouseOfficer.apply)
  implicit val writes: Writes[CompaniesHouseOfficer] = Json.writes[CompaniesHouseOfficer]
  implicit val format: Format[CompaniesHouseOfficer] = Format(reads, writes)

  def fromString(s: String): CompaniesHouseOfficer = {
    val parts = s.split('|')
    val name = parts(0)
    val dobParts =
      if (parts.length > 1)
        parts(1).split('/')
      else
        Array("", "", "")
    val day =
      if (dobParts(0).isEmpty)
        None
      else
        Some(dobParts(0).toInt)
    val month =
      if (dobParts.length > 1 && dobParts(1).nonEmpty)
        dobParts(1).toInt
      else
        0
    val year =
      if (dobParts.length > 2 && dobParts(2).nonEmpty)
        dobParts(2).toInt
      else
        0
    val dateOfBirth =
      if (month != 0 && year != 0)
        Some(CompaniesHouseDateOfBirth(
          day,
          month,
          year
        ))
      else
        None
    CompaniesHouseOfficer(name, dateOfBirth)
  }
