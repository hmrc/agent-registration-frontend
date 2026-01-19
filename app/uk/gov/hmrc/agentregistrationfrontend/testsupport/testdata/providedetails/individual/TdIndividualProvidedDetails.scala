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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.providedetails.individual

import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseMatch
import uk.gov.hmrc.agentregistration.shared.llp.IndividualNino
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.llp.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.llp.IndividualVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.llp.ProvidedDetailsState.Finished
import uk.gov.hmrc.agentregistration.shared.llp.ProvidedDetailsState.Started
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase

trait TdIndividualProvidedDetails { dependencies: (TdBase) =>

  object providedDetailsLlp:

    val afterStarted: IndividualProvidedDetails = IndividualProvidedDetails(
      _id = dependencies.individualProvidedDetailsId,
      internalUserId = dependencies.internalUserId,
      createdAt = dependencies.nowAsInstant,
      agentApplicationId = dependencies.agentApplicationId,
      providedDetailsState = Started
    )

    val afterNameQueryProvided: IndividualProvidedDetails = afterStarted
      .modify(_.companiesHouseMatch)
      .setTo(
        Some(CompaniesHouseMatch(
          memberNameQuery = dependencies.llpNameQuery,
          companiesHouseOfficer = None
        ))
      )

    val afterOfficerChosen: IndividualProvidedDetails = afterStarted
      .modify(_.companiesHouseMatch)
      .setTo(
        Some(CompaniesHouseMatch(
          memberNameQuery = dependencies.llpNameQuery,
          companiesHouseOfficer = Some(dependencies.companiesHouseOfficer)
        ))
      )

    val afterTelephoneNumberProvided: IndividualProvidedDetails = afterOfficerChosen
      .modify(_.telephoneNumber)
      .setTo(Some(dependencies.telephoneNumber))

    val afterEmailAddressProvided: IndividualProvidedDetails = afterTelephoneNumberProvided
      .modify(_.emailAddress)
      .setTo(Some(IndividualVerifiedEmailAddress(
        emailAddress = dependencies.individualEmailAddress,
        isVerified = false
      )))

    val afterEmailAddressVerified: IndividualProvidedDetails = afterTelephoneNumberProvided
      .modify(_.emailAddress)
      .setTo(Some(IndividualVerifiedEmailAddress(
        emailAddress = dependencies.individualEmailAddress,
        isVerified = true
      )))

    object AfterNino:

      val afterNinoProvided: IndividualProvidedDetails = afterEmailAddressVerified
        .modify(_.individualNino)
        .setTo(Some(dependencies.ninoProvided))

      val afterNinoFromAuth: IndividualProvidedDetails = afterEmailAddressVerified
        .modify(_.individualNino)
        .setTo(Some(dependencies.ninoFromAuth))

      val afterNinoNotProvided: IndividualProvidedDetails = afterEmailAddressVerified
        .modify(_.individualNino)
        .setTo(Some(IndividualNino.NotProvided))

    object AfterSaUtr:

      val afterSaUtrProvided: IndividualProvidedDetails = AfterNino.afterNinoProvided
        .modify(_.individualSaUtr)
        .setTo(Some(dependencies.saUtrProvided))

      val afterSaUtrFromAuth: IndividualProvidedDetails = AfterNino.afterNinoFromAuth
        .modify(_.individualSaUtr)
        .setTo(Some(dependencies.saUtrFromAuth))

      val afterSaUtrFromCitizenDetails: IndividualProvidedDetails = AfterNino.afterNinoFromAuth
        .modify(_.individualSaUtr)
        .setTo(Some(dependencies.saUtrFromCitizenDetails))

      val afterSaUtrNotProvided: IndividualProvidedDetails = AfterNino.afterNinoNotProvided
        .modify(_.individualSaUtr)
        .setTo(Some(IndividualSaUtr.NotProvided))

    val afterApproveAgentApplication: IndividualProvidedDetails = AfterSaUtr.afterSaUtrProvided
      .modify(_.hasApprovedApplication)
      .setTo(Some(true))

    val afterDoNotApproveAgentApplication: IndividualProvidedDetails = AfterSaUtr.afterSaUtrProvided
      .modify(_.hasApprovedApplication)
      .setTo(Some(false))

    val afterHmrcStandardforAgentsAgreed: IndividualProvidedDetails = afterApproveAgentApplication
      .modify(_.hmrcStandardForAgentsAgreed)
      .setTo(StateOfAgreement.Agreed)

    val afterProvidedDetailsConfirmed: IndividualProvidedDetails = afterHmrcStandardforAgentsAgreed
      .modify(_.providedDetailsState)
      .setTo(Finished)

}
