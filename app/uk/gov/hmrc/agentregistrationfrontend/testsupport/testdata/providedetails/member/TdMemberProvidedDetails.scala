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
import uk.gov.hmrc.agentregistration.shared.llp.MemberNino
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistration.shared.llp.MemberSaUtr
import uk.gov.hmrc.agentregistration.shared.llp.MemberVerifiedEmailAddress
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

    val afterTelephoneNumberProvided: MemberProvidedDetails = afterOfficerChosen
      .modify(_.telephoneNumber)
      .setTo(Some(dependencies.telephoneNumber))

    val afterEmailAddressProvided: MemberProvidedDetails = afterTelephoneNumberProvided
      .modify(_.emailAddress)
      .setTo(Some(MemberVerifiedEmailAddress(
        emailAddress = dependencies.memberEmailAddress,
        isVerified = false
      )))

    val afterEmailAddressVerified: MemberProvidedDetails = afterTelephoneNumberProvided
      .modify(_.emailAddress)
      .setTo(Some(MemberVerifiedEmailAddress(
        emailAddress = dependencies.memberEmailAddress,
        isVerified = true
      )))

    // That way is better than having to write a bunch of methods for each field in MemberProvidedDetails
    def withNinoProvided(state: MemberProvidedDetails): MemberProvidedDetails = state
      .modify(_.memberNino)
      .setTo(Some(dependencies.ninoProvided))

    def withNinoFromAuth(state: MemberProvidedDetails): MemberProvidedDetails = state
      .modify(_.memberNino)
      .setTo(Some(dependencies.ninoFromAuth))

    def withNinoNotProvided(state: MemberProvidedDetails): MemberProvidedDetails = state
      .modify(_.memberNino)
      .setTo(Some(MemberNino.NotProvided))

    val afterNinoProvided: MemberProvidedDetails = afterEmailAddressProvided
      .modify(_.memberNino)
      .setTo(Some(dependencies.ninoProvided))

    def withSaUtrProvided(state: MemberProvidedDetails): MemberProvidedDetails = state
      .modify(_.memberSaUtr)
      .setTo(Some(dependencies.saUtrProvided))

    def withSaUtrFromAuth(state: MemberProvidedDetails): MemberProvidedDetails = state
      .modify(_.memberSaUtr)
      .setTo(Some(dependencies.saUtrFromAuth))

    def withSaUtrFromCitizenDetails(state: MemberProvidedDetails): MemberProvidedDetails = state
      .modify(_.memberSaUtr)
      .setTo(Some(dependencies.saUtrFromCitizenDetails))

    def withSaUtrNotProvided(state: MemberProvidedDetails): MemberProvidedDetails = state
      .modify(_.memberSaUtr)
      .setTo(Some(MemberSaUtr.NotProvided))

    val afterSaUtrProvided: MemberProvidedDetails = afterNinoProvided
      .modify(_.memberSaUtr)
      .setTo(Some(dependencies.saUtrProvided))

    val afterApproveAgentApplication: MemberProvidedDetails = afterSaUtrProvided
      .modify(_.hasApprovedApplication)
      .setTo(Some(true))

    val afterDoNotApproveAgentApplication: MemberProvidedDetails = afterSaUtrProvided
      .modify(_.hasApprovedApplication)
      .setTo(Some(false))

}
