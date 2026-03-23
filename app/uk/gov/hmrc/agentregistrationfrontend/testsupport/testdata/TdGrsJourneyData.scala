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

import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyData
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyId
import uk.gov.hmrc.agentregistrationfrontend.model.grs.Registration
import uk.gov.hmrc.agentregistrationfrontend.model.grs.RegistrationStatus

trait TdGrsJourneyData {
  dependencies: TdBase =>

  object grsJourneyData:

    val journeyId: JourneyId = JourneyId("1234567890-8888")

    object llp:

      val journeyData: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = Some(dependencies.companyProfile),
        ctutr = None,
        postcode = Some(dependencies.postcode),
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

    object soleTrader:

      val journeyData: JourneyData = JourneyData(
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

    object ltd:

      val journeyData: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = None,
        companyProfile = Some(dependencies.companyProfile),
        ctutr = Some(dependencies.ctUtr),
        postcode = None,
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

    object ltdPartnership:

      val journeyData: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = Some(dependencies.companyProfile),
        ctutr = None,
        postcode = Some(dependencies.postcode),
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

    object scottishLtdPartnership:

      val journeyData: JourneyData = JourneyData(
        fullName = None,
        dateOfBirth = None,
        nino = None,
        trn = None,
        sautr = Some(dependencies.saUtr),
        companyProfile = Some(dependencies.companyProfile),
        ctutr = None,
        postcode = Some(dependencies.postcode),
        identifiersMatch = true,
        registration = Registration(
          registrationStatus = RegistrationStatus.GrsRegistered,
          registeredBusinessPartnerId = Some(dependencies.safeId)
        )
      )

    object generalPartnership:

      val journeyData: JourneyData = JourneyData(
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

    object scottishPartnership:

      val journeyData: JourneyData = JourneyData(
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

}
