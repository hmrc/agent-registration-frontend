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

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.BusinessType.GeneralPartnership
import uk.gov.hmrc.agentregistration.shared.BusinessType.LimitedCompany
import uk.gov.hmrc.agentregistration.shared.BusinessType.LimitedLiabilityPartnership
import uk.gov.hmrc.agentregistration.shared.BusinessType.LimitedPartnership
import uk.gov.hmrc.agentregistration.shared.BusinessType.ScottishLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.BusinessType.ScottishPartnership
import uk.gov.hmrc.agentregistration.shared.BusinessType.SoleTrader
import uk.gov.hmrc.agentregistrationfrontend.model.grs.Registration.given

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
  sautr: Option[Utr], // sole trader / partnership (both can return None but that will not have a safeId)
  companyProfile: Option[CompanyProfile], // limited company or any limited partnership
  ctutr: Option[Utr], // limited company
  postcode: Option[String], // any partnership
  identifiersMatch: Boolean,
  registration: Registration
):

  // TODO: distinguish between CT and SA Utrs, make dedicated types and analyse when to use correct identifier
  def getUtr: Utr = sautr.orElse(ctutr).getOrElse(throw new Exception("Business details missing Utr"))

  def toBusinessDetails(businessType: BusinessType): BusinessDetails = {
    def missingDataError(key: String): Nothing = throw new RuntimeException(s"Business details missing $key for $businessType type")

    businessType match {
      case LimitedCompany =>
        LimitedCompanyDetails(
          safeId = registration.registeredBusinessPartnerId.getOrElse(missingDataError("safeId")),
          businessType = businessType,
          companyProfile = companyProfile.getOrElse(missingDataError("companyProfile"))
        )
      case SoleTrader =>
        SoleTraderDetails(
          safeId = registration.registeredBusinessPartnerId.getOrElse(missingDataError("safeId")),
          businessType = businessType,
          fullName = fullName.getOrElse(missingDataError("fullName")),
          dateOfBirth = dateOfBirth.getOrElse(missingDataError("dateOfBirth")),
          nino = nino,
          trn = trn
        )
      case GeneralPartnership | LimitedLiabilityPartnership | LimitedPartnership | ScottishLimitedPartnership | ScottishPartnership =>
        PartnershipDetails(
          safeId = registration.registeredBusinessPartnerId.getOrElse(missingDataError("safeId")),
          businessType = businessType,
          companyProfile = companyProfile,
          postcode = postcode.getOrElse(missingDataError("postcode"))
        )
    }
  }

object JourneyData:
  given Format[JourneyData] = Json.format[JourneyData]
