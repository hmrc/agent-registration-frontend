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

package uk.gov.hmrc.agentregistrationfrontend.model.grs

import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLimitedCompany
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsPartnership
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsScottishPartnership
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsSoleTrader
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.businessdetails.FullName

import java.time.LocalDate

// TODO sole trader responses can also contain an overseas address and an overseas taxIdentifier, do we do anything about this?

/** This represents uber class to hold all cases of journey data for various business types. In reality many fields are set only for specific business types,
  * others are left none. TODO: consider creating dedicated classes for each endpoint with specified fields, e.g. SoleTraderJourneyData, PartnershipJourneyData,
  * etc.
  */
final case class JourneyData(
  fullName: Option[FullName], // sole trader
  dateOfBirth: Option[LocalDate], // sole trader
  nino: Option[Nino], // sole trader (can be replaced by trn)
  trn: Option[String], // sole trader (if this is present then there will also be an 'address' and 'saPostcode' field)
  sautr: Option[SaUtr], // sole trader / partnership (both can return None but that will not have a safeId)
  companyProfile: Option[CompanyProfile], // limited company or any limited partnership
  ctutr: Option[CtUtr], // limited company
  postcode: Option[String], // any partnership
  identifiersMatch: Boolean,
  registration: Registration
)

object JourneyData:

  given Format[JourneyData] = Json.format[JourneyData]

  extension (journeyData: JourneyData)

    def asBusinessDetailsSoleTrader: BusinessDetailsSoleTrader = BusinessDetailsSoleTrader(
      safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
      saUtr = journeyData.sautr.getOrThrowExpectedDataMissing("sautr"),
      fullName = journeyData.fullName.getOrThrowExpectedDataMissing("fullName"),
      dateOfBirth = journeyData.dateOfBirth.getOrThrowExpectedDataMissing("dateOfBirth"),
      nino = journeyData.nino,
      trn = journeyData.trn
    )

    def asBusinessDetailsLlp: BusinessDetailsLlp = BusinessDetailsLlp(
      safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
      saUtr = journeyData.sautr.getOrThrowExpectedDataMissing("sautr"),
      companyProfile = journeyData.companyProfile.getOrThrowExpectedDataMissing("companyProfile")
    )

    def asBusinessDetailsLimitedCompany: BusinessDetailsLimitedCompany = BusinessDetailsLimitedCompany(
      safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
      ctUtr = journeyData.ctutr.getOrThrowExpectedDataMissing("ctutr"),
      companyProfile = journeyData.companyProfile.getOrThrowExpectedDataMissing("companyProfile")
    )

    def asBusinessDetailsPartnership: BusinessDetailsPartnership = BusinessDetailsPartnership(
      safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
      saUtr = journeyData.sautr.getOrThrowExpectedDataMissing("sautr"),
      companyProfile = journeyData.companyProfile.getOrThrowExpectedDataMissing("companyProfile"),
      postcode = journeyData.postcode.getOrThrowExpectedDataMissing("postcode")
    )

    def asBusinessScottishPartnership: BusinessDetailsScottishPartnership = BusinessDetailsScottishPartnership(
      safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
      saUtr = journeyData.sautr.getOrThrowExpectedDataMissing("sautr"),
      postcode = journeyData.postcode.getOrThrowExpectedDataMissing("postcode")
    )

    def asBusinessDetailsGeneralPartnership: BusinessDetailsGeneralPartnership = BusinessDetailsGeneralPartnership(
      safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
      saUtr = journeyData.sautr.getOrThrowExpectedDataMissing("sautr"),
      postcode = journeyData.postcode.getOrThrowExpectedDataMissing("postcode")
    )
