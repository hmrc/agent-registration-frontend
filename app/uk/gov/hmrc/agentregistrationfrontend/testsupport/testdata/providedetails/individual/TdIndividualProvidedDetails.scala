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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Finished
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Started
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase

trait TdIndividualProvidedDetails { dependencies: (TdBase) =>

  object providedDetailsLlp:

    val afterStarted: IndividualProvidedDetailsToBeDeleted = IndividualProvidedDetailsToBeDeleted(
      _id = dependencies.individualProvidedDetailsId,
      internalUserId = dependencies.internalUserId,
      createdAt = dependencies.nowAsInstant,
      agentApplicationId = dependencies.agentApplicationId,
      providedDetailsState = Started
    )

    val afterNameQueryProvided: IndividualProvidedDetailsToBeDeleted = afterStarted
      .modify(_.companiesHouseMatch)
      .setTo(
        Some(CompaniesHouseMatch(
          memberNameQuery = dependencies.llpNameQuery,
          companiesHouseOfficer = None
        ))
      )

    val afterOfficerChosen: IndividualProvidedDetailsToBeDeleted = afterStarted
      .modify(_.companiesHouseMatch)
      .setTo(
        Some(CompaniesHouseMatch(
          memberNameQuery = dependencies.llpNameQuery,
          companiesHouseOfficer = Some(dependencies.companiesHouseOfficer)
        ))
      )

    val afterTelephoneNumberProvided: IndividualProvidedDetailsToBeDeleted = afterOfficerChosen
      .modify(_.telephoneNumber)
      .setTo(Some(dependencies.telephoneNumber))

    val afterEmailAddressProvided: IndividualProvidedDetailsToBeDeleted = afterTelephoneNumberProvided
      .modify(_.emailAddress)
      .setTo(Some(IndividualVerifiedEmailAddress(
        emailAddress = dependencies.individualEmailAddress,
        isVerified = false
      )))

    val afterEmailAddressVerified: IndividualProvidedDetailsToBeDeleted = afterTelephoneNumberProvided
      .modify(_.emailAddress)
      .setTo(Some(IndividualVerifiedEmailAddress(
        emailAddress = dependencies.individualEmailAddress,
        isVerified = true
      )))

    object AfterDateOfBirth:

      val afterDateOfBirthProvided: IndividualProvidedDetailsToBeDeleted = afterEmailAddressVerified
        .modify(_.individualDateOfBirth)
        .setTo(Some(dependencies.dateOfBirthProvided))

      val afterDateOfBirthFromCitizenDetails: IndividualProvidedDetailsToBeDeleted = afterEmailAddressVerified
        .modify(_.individualDateOfBirth)
        .setTo(Some(dependencies.dateOfBirthFromCitizenDetails))

    object AfterNino:

      val afterNinoProvided: IndividualProvidedDetailsToBeDeleted = AfterDateOfBirth.afterDateOfBirthProvided
        .modify(_.individualNino)
        .setTo(Some(dependencies.ninoProvided))

      val afterNinoFromAuth: IndividualProvidedDetailsToBeDeleted = AfterDateOfBirth.afterDateOfBirthFromCitizenDetails
        .modify(_.individualNino)
        .setTo(Some(dependencies.ninoFromAuth))

      val afterNinoNotProvided: IndividualProvidedDetailsToBeDeleted = afterEmailAddressVerified
        .modify(_.individualNino)
        .setTo(Some(IndividualNino.NotProvided))

    object AfterSaUtr:

      val afterSaUtrProvided: IndividualProvidedDetailsToBeDeleted = AfterNino.afterNinoProvided
        .modify(_.individualSaUtr)
        .setTo(Some(dependencies.saUtrProvided))

      val afterSaUtrFromAuth: IndividualProvidedDetailsToBeDeleted = AfterNino.afterNinoFromAuth
        .modify(_.individualSaUtr)
        .setTo(Some(dependencies.saUtrFromAuth))

      val afterSaUtrFromCitizenDetails: IndividualProvidedDetailsToBeDeleted = AfterNino.afterNinoFromAuth
        .modify(_.individualSaUtr)
        .setTo(Some(dependencies.saUtrFromCitizenDetails))

      val afterSaUtrNotProvided: IndividualProvidedDetailsToBeDeleted = AfterNino.afterNinoNotProvided
        .modify(_.individualSaUtr)
        .setTo(Some(IndividualSaUtr.NotProvided))

    val afterApproveAgentApplication: IndividualProvidedDetailsToBeDeleted = AfterSaUtr.afterSaUtrProvided
      .modify(_.hasApprovedApplication)
      .setTo(Some(true))

    val afterDoNotApproveAgentApplication: IndividualProvidedDetailsToBeDeleted = AfterSaUtr.afterSaUtrProvided
      .modify(_.hasApprovedApplication)
      .setTo(Some(false))

    val afterHmrcStandardforAgentsAgreed: IndividualProvidedDetailsToBeDeleted = afterApproveAgentApplication
      .modify(_.hmrcStandardForAgentsAgreed)
      .setTo(StateOfAgreement.Agreed)

    val afterProvidedDetailsConfirmed: IndividualProvidedDetailsToBeDeleted = afterHmrcStandardforAgentsAgreed
      .modify(_.providedDetailsState)
      .setTo(Finished)

}
