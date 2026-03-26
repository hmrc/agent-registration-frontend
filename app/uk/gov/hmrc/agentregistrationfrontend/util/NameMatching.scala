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
  ): Option[IndividualName] = companiesHouseOfficerList.find(officer =>
    officer.value.toLowerCase === individualName.value.toLowerCase
  )

  def filterAlreadyUsedNames(
    allCompaniesHouseOfficerNames: Seq[IndividualName],
    existingIndividualNames: Seq[IndividualName]
  ): Seq[IndividualName] =
    val existingNamesLower = existingIndividualNames.map(_.value.toLowerCase)
    allCompaniesHouseOfficerNames
      .foldLeft((Seq.empty[IndividualName], existingNamesLower)):
        case ((kept, remaining), chName) =>
          val idx = remaining.indexOf(chName.value.toLowerCase)
          if idx >= 0 then (kept, remaining.patch(idx, Nil, 1))
          else (kept :+ chName, remaining)
      ._1

}
