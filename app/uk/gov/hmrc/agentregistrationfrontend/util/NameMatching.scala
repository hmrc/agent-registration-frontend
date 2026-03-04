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

package uk.gov.hmrc.agentregistrationfrontend.util

import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

object NameMatching {
  def individualNameMatching(
    individualName: IndividualName,
    companiesHouseOfficerList: Seq[IndividualName]
  ): Option[IndividualName] =
    val maybeExactMatch = companiesHouseOfficerList.find(officer =>
      officer.value.toLowerCase === individualName.value.toLowerCase
    )
    maybeExactMatch match
      case Some(officer) => Some(officer)
      case None =>
        val inputSurname = individualName.value.split(" ").lastOption.map(_.toLowerCase)
        val surnameMatches = companiesHouseOfficerList.filter(officer =>
          officer.value.split(" ").lastOption.map(_.toLowerCase) === inputSurname
        )
        surnameMatches match
          case Seq(singleMatch) => Some(singleMatch)
          case Seq() => None
          case multipleMatches =>
            val inputFirstName = individualName.value.split(" ").headOption.map(_.toLowerCase)
            multipleMatches.find(officer =>
              officer.value.split(" ").headOption.map(_.toLowerCase) === inputFirstName
            )
}
