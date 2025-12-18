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
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
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

    object AfterNinoProvided:

      val afterNinoProvided: MemberProvidedDetails = afterEmailAddressVerified
        .modify(_.memberNino)
        .setTo(Some(dependencies.ninoProvided))

      val afterNinoFromAuth: MemberProvidedDetails = afterEmailAddressVerified
        .modify(_.memberNino)
        .setTo(Some(dependencies.ninoFromAuth))

      val afterNinoNotProvided: MemberProvidedDetails = afterEmailAddressVerified
        .modify(_.memberNino)
        .setTo(Some(MemberNino.NotProvided))

    object AfterSaUtrProvided:

      val afterSaUtrProvided: MemberProvidedDetails = AfterNinoProvided.afterNinoProvided
        .modify(_.memberSaUtr)
        .setTo(Some(dependencies.saUtrProvided))

      val afterSaUtrFromAuth: MemberProvidedDetails = AfterNinoProvided.afterNinoFromAuth
        .modify(_.memberSaUtr)
        .setTo(Some(dependencies.saUtrFromAuth))

      val afterSaUtrFromCitizenDetails: MemberProvidedDetails = AfterNinoProvided.afterNinoFromAuth
        .modify(_.memberSaUtr)
        .setTo(Some(dependencies.saUtrFromCitizenDetails))

      val afterSaUtrNotProvided: MemberProvidedDetails = AfterNinoProvided.afterNinoNotProvided
        .modify(_.memberSaUtr)
        .setTo(Some(MemberSaUtr.NotProvided))

    val afterApproveAgentApplication: MemberProvidedDetails = AfterSaUtrProvided.afterSaUtrProvided
      .modify(_.hasApprovedApplication)
      .setTo(Some(true))

    val afterDoNotApproveAgentApplication: MemberProvidedDetails = AfterSaUtrProvided.afterSaUtrProvided
      .modify(_.hasApprovedApplication)
      .setTo(Some(false))

    val afterHmrcStandardforAgentsAgreed: MemberProvidedDetails = afterApproveAgentApplication
      .modify(_.hmrcStandardForAgentsAgreed)
      .setTo(StateOfAgreement.Agreed)

}
