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

package uk.gov.hmrc.agentregistration.shared.testdata.providedetails.individual

import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement.Agreed
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.AccessConfirmed
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Finished
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Precreated
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Started
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.IndividualData
import uk.gov.hmrc.agentregistration.shared.testdata.TdApplicationIdentifiers
import uk.gov.hmrc.agentregistration.shared.testdata.TdDates
import uk.gov.hmrc.agentregistration.shared.testdata.TdIndividualIdentifiers

object TdIndividualInStates:
  def make(
    seed: String,
    agentApplicationId: AgentApplicationId
  ): TdIndividualInStates =
    new TdIndividualInStates:
      override val tdIdentifiers: TdIndividualIdentifiers = TdIndividualIdentifiers.make(seed, agentApplicationId)

trait TdIndividualInStates:

  def tdIdentifiers: TdIndividualIdentifiers

  val precreated: IndividualProvidedDetails = IndividualProvidedDetails(
    _id = tdIdentifiers.individualProvidedDetailsId,
    personReference = tdIdentifiers.personReference,
    internalUserId = None,
    individualName = tdIdentifiers.individualName,
    createdAt = TdDates.instant20DaysLater,
    agentApplicationId = tdIdentifiers.agentApplicationId,
    providedDetailsState = Precreated,
    isPersonOfControl = true,
    passedIv = None
  )

  val afterAccessConfirmed: IndividualProvidedDetails = precreated.copy(
    providedDetailsState = AccessConfirmed
  )

  val afterStarted: IndividualProvidedDetails = afterAccessConfirmed.copy(
    internalUserId = Some(tdIdentifiers.internalUserId),
    providedDetailsState = Started,
    passedIv = Some(true)
  )

  val afterTelephoneNumberProvided: IndividualProvidedDetails = afterStarted
    .modify(_.telephoneNumber)
    .setTo(Some(tdIdentifiers.telephoneNumber))

  val afterEmailAddressProvided: IndividualProvidedDetails = afterTelephoneNumberProvided
    .modify(_.emailAddress)
    .setTo(Some(IndividualVerifiedEmailAddress(
      emailAddress = tdIdentifiers.individualEmailAddress,
      isVerified = false
    )))

  val afterEmailAddressVerified: IndividualProvidedDetails = afterTelephoneNumberProvided
    .modify(_.emailAddress)
    .setTo(Some(IndividualVerifiedEmailAddress(
      emailAddress = tdIdentifiers.individualEmailAddress,
      isVerified = true
    )))

  object AfterDateOfBirth:

    val afterDateOfBirthProvided: IndividualProvidedDetails = afterEmailAddressVerified
      .modify(_.individualDateOfBirth)
      .setTo(Some(tdIdentifiers.dateOfBirthProvided))

    val afterDateOfBirthFromCitizenDetails: IndividualProvidedDetails = afterEmailAddressVerified
      .modify(_.individualDateOfBirth)
      .setTo(Some(tdIdentifiers.dateOfBirthFromCitizenDetails))

  object AfterNino:

    val afterNinoProvided: IndividualProvidedDetails = AfterDateOfBirth.afterDateOfBirthProvided
      .modify(_.individualNino)
      .setTo(Some(tdIdentifiers.ninoProvided))

    val afterNinoFromAuth: IndividualProvidedDetails = AfterDateOfBirth.afterDateOfBirthFromCitizenDetails
      .modify(_.individualNino)
      .setTo(Some(tdIdentifiers.ninoFromAuth))

    val afterNinoNotProvided: IndividualProvidedDetails = afterEmailAddressVerified
      .modify(_.individualNino)
      .setTo(Some(IndividualNino.NotProvided))

  object AfterSaUtr:

    val afterSaUtrProvided: IndividualProvidedDetails = AfterNino.afterNinoProvided
      .modify(_.individualSaUtr)
      .setTo(Some(tdIdentifiers.saUtrProvided))

    val afterSaUtrFromAuth: IndividualProvidedDetails = AfterNino.afterNinoFromAuth
      .modify(_.individualSaUtr)
      .setTo(Some(tdIdentifiers.saUtrFromAuth))

    val afterSaUtrFromCitizenDetails: IndividualProvidedDetails = AfterNino.afterNinoFromAuth
      .modify(_.individualSaUtr)
      .setTo(Some(tdIdentifiers.saUtrFromCitizenDetails))

    val afterSaUtrNotProvided: IndividualProvidedDetails = AfterNino.afterNinoNotProvided
      .modify(_.individualSaUtr)
      .setTo(Some(IndividualSaUtr.NotProvided))

  val afterUcrProvided: IndividualProvidedDetails = AfterSaUtr.afterSaUtrProvided
    .copy(
      vrns = Some(List(tdIdentifiers.vrn)),
      payeRefs = Some(List(tdIdentifiers.payeRef))
    )

  val afterUcrProvidedNotProvide: IndividualProvidedDetails = AfterSaUtr.afterSaUtrProvided
    .copy(
      vrns = Some(List.empty),
      payeRefs = Some(List.empty)
    )

  val afterApproveAgentApplication: IndividualProvidedDetails = afterUcrProvided
    .modify(_.hasApprovedApplication)
    .setTo(Some(true))

  val afterDoNotApproveAgentApplication: IndividualProvidedDetails = afterUcrProvided
    .modify(_.hasApprovedApplication)
    .setTo(Some(false))

  val afterHmrcStandardforAgentsAgreed: IndividualProvidedDetails = afterApproveAgentApplication
    .modify(_.hmrcStandardForAgentsAgreed)
    .setTo(StateOfAgreement.Agreed)

  val afterFinished: IndividualProvidedDetails = afterHmrcStandardforAgentsAgreed
    .modify(_.providedByApplicant)
    .setTo(Some(false))
    .modify(_.providedDetailsState)
    .setTo(Finished)

  val individualData: IndividualData = IndividualData(
    personReference = tdIdentifiers.personReference,
    individualName = tdIdentifiers.individualName,
    isPersonOfControl = true,
    individualDateOfBirth = tdIdentifiers.dateOfBirthProvided,
    telephoneNumber = tdIdentifiers.telephoneNumber,
    emailAddress = tdIdentifiers.individualEmailAddress,
    individualNino = tdIdentifiers.ninoProvided,
    individualSaUtr = tdIdentifiers.saUtrProvided,
    vrns = List(tdIdentifiers.vrn),
    payeRefs = List(tdIdentifiers.payeRef),
    passedIv = true,
    providedByApplicant = false
  )

//  object soleTrader:
//
//    val soleTraderAutopopulatedDetails: IndividualProvidedDetails = precreated.copy(
//      providedDetailsState = ProvidedDetailsState.AccessConfirmed,
//      telephoneNumber = Some(tdIdentifiers.telephoneNumber),
//      emailAddress = Some(IndividualVerifiedEmailAddress(tdIdentifiers.applicantEmailAddress, isVerified = true)),
//      hasApprovedApplication = Some(true),
//      hmrcStandardForAgentsAgreed = Agreed,
//      providedByApplicant = Some(false),
//      isPersonOfControl = true,
//      passedIv = None
//    )
