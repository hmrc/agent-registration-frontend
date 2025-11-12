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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.providedetails.member

import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseMatch
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistration.shared.llp.ProvidedDetailsState.Started
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase

trait TdMemberProvidedDetails { dependencies: (TdBase) =>

  object providedDetailsLlp:

    val afterStarted: MemberProvidedDetails = MemberProvidedDetails(
      _id = dependencies.memberProvidedDetailsId,
      internalUserId = dependencies.internalUserId,
      createdAt = dependencies.nowAsInstant,
      agentApplicationId = dependencies.agentApplicationId,
      providedDetailsState = Started
    )

    val afterNameQueryProvided: MemberProvidedDetails = afterStarted
      .modify(_.companiesHouseMatch)
      .setTo(
        Some(CompaniesHouseMatch(
          memberNameQuery = dependencies.llpNameQuery,
          companiesHouseOfficer = None
        ))
      )

    val afterOfficerChosen: MemberProvidedDetails = afterStarted
      .modify(_.companiesHouseMatch)
      .setTo(
        Some(CompaniesHouseMatch(
          memberNameQuery = dependencies.llpNameQuery,
          companiesHouseOfficer = Some(dependencies.companiesHouseOfficer)
        ))
      )

}
