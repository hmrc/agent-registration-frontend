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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLimitedCompany
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsPartnership
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsScottishPartnership
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsSoleTrader
import uk.gov.hmrc.agentregistration.shared.businessdetails.FullName
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationfrontend.model.grs.*

import java.time.LocalDate

trait TdGrs {
  dependencies: TdBase =>

  object grs:

    val journeyId: JourneyId = JourneyId("1234567890-8888")

    object llp:

      val journeyDataBase: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = Some(dependencies.companyProfile()),
        ctutr = None,
        postcode = Some(dependencies.postcode),
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

      val businessDetails = BusinessDetailsLlp(
        safeId = journeyDataBase.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
        saUtr = journeyDataBase.sautr.getOrThrowExpectedDataMissing("sautr"),
        companyProfile = journeyDataBase.companyProfile.getOrThrowExpectedDataMissing("companyProfile")
      )

    object soleTrader:

      val fullName: FullName = FullName(firstName = "ST Name", lastName = "ST Lastname")
      val dateOfBirth: LocalDate = LocalDate.of(1990, 1, 2)
      val journeyDataBase: JourneyData = JourneyData(
        fullName = Some(fullName),
        dateOfBirth = Some(dateOfBirth),
        nino = Some(dependencies.nino),
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = None,
        ctutr = None,
        postcode = None,
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

      val businessDetails = BusinessDetailsSoleTrader(
        safeId = journeyDataBase.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
        saUtr = journeyDataBase.sautr.getOrThrowExpectedDataMissing("sautr"),
        fullName = journeyDataBase.fullName.getOrThrowExpectedDataMissing("fullName"),
        dateOfBirth = journeyDataBase.dateOfBirth.getOrThrowExpectedDataMissing("dateOfBirth"),
        nino = journeyDataBase.nino,
        trn = journeyDataBase.trn
      )

    object ltd:

      val journeyDataBase: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = None,
        companyProfile = Some(dependencies.companyProfile()),
        ctutr = Some(dependencies.ctUtr),
        postcode = None,
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

      val businessDetails: BusinessDetailsLimitedCompany = BusinessDetailsLimitedCompany(
        safeId = journeyDataBase.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
        ctUtr = journeyDataBase.ctutr.getOrThrowExpectedDataMissing("ctutr"),
        companyProfile = journeyDataBase.companyProfile.getOrThrowExpectedDataMissing("companyProfile")
      )

    object ltdPartnership:

      val journeyDataBase: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = Some(dependencies.companyProfile()),
        ctutr = None,
        postcode = Some(dependencies.postcode),
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

      val businessDetails: BusinessDetailsPartnership = BusinessDetailsPartnership(
        safeId = journeyDataBase.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
        saUtr = journeyDataBase.sautr.getOrThrowExpectedDataMissing("saUtr"),
        postcode = journeyDataBase.postcode.getOrThrowExpectedDataMissing("postcode"),
        companyProfile = journeyDataBase.companyProfile.getOrThrowExpectedDataMissing("companyProfile")
      )

    object scottishLtdPartnership:

      val journeyDataBase: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = Some(dependencies.companyProfile()),
        ctutr = None,
        postcode = Some(dependencies.postcode),
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

      val businessDetails: BusinessDetailsPartnership = BusinessDetailsPartnership(
        safeId = journeyDataBase.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
        saUtr = journeyDataBase.sautr.getOrThrowExpectedDataMissing("saUtr"),
        postcode = journeyDataBase.postcode.getOrThrowExpectedDataMissing("postcode"),
        companyProfile = journeyDataBase.companyProfile.getOrThrowExpectedDataMissing("companyProfile")
      )

    object generalPartnership:

      val journeyDataBase: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = None,
        ctutr = None,
        postcode = Some(dependencies.postcode),
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

      val businessDetails: BusinessDetailsGeneralPartnership = BusinessDetailsGeneralPartnership(
        safeId = journeyDataBase.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
        saUtr = journeyDataBase.sautr.getOrThrowExpectedDataMissing("sautr"),
        postcode = journeyDataBase.postcode.getOrThrowExpectedDataMissing("postcode")
      )

    object scottishPartnership:

      val journeyDataBase: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = None,
        ctutr = None,
        postcode = Some(dependencies.postcode),
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

      val businessDetails: BusinessDetailsScottishPartnership = BusinessDetailsScottishPartnership(
        safeId = journeyDataBase.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
        saUtr = journeyDataBase.sautr.getOrThrowExpectedDataMissing("sautr"),
        postcode = journeyDataBase.postcode.getOrThrowExpectedDataMissing("postcode")
      )

}
