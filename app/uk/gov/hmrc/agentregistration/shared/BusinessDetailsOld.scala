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

package uk.gov.hmrc.agentregistration.shared

import play.api.libs.json.Format
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.BusinessType.*
import uk.gov.hmrc.agentregistration.shared.BusinessType.Partnership.*
import uk.gov.hmrc.agentregistration.shared.businessdetails.CompanyProfile
import uk.gov.hmrc.agentregistration.shared.businessdetails.FullName

import java.time.LocalDate

sealed trait OldBusinessDetails:

  val safeId: SafeId
  val businessType: BusinessType // Duplicated from AgentApplication to simplify json reads

object OldBusinessDetails:

  given Format[OldBusinessDetails] = Format(
    { json =>
      (json \ "businessType").as[BusinessType] match {
        case LimitedCompany => Json.fromJson[OldLimitedCompanyDetails](json)
        case SoleTrader => Json.fromJson[OldSoleTraderDetails](json)
        case GeneralPartnership | LimitedLiabilityPartnership | LimitedPartnership | ScottishLimitedPartnership | ScottishPartnership =>
          Json.fromJson[OldPartnershipDetails](json)
      }
    },
    {
      case limitedCompany: OldLimitedCompanyDetails => Json.toJson(limitedCompany)
      case soleTrader: OldSoleTraderDetails => Json.toJson(soleTrader)
      case partnership: OldPartnershipDetails => Json.toJson(partnership)
    }
  )

final case class OldLimitedCompanyDetails(
  safeId: SafeId,
  businessType: BusinessType = LimitedCompany,
  ctUtr: CtUtr,
  companyProfile: CompanyProfile
)
extends OldBusinessDetails

object OldLimitedCompanyDetails:
  given Format[OldLimitedCompanyDetails] = Json.format[OldLimitedCompanyDetails]

final case class OldSoleTraderDetails(
  safeId: SafeId,
  businessType: BusinessType = SoleTrader,
  saUtr: SaUtr,
  fullName: FullName,
  dateOfBirth: LocalDate,
  nino: Option[Nino],
  trn: Option[String]
  // saPostcode (only when trn present)
  // address (only when trn present)
  // overseas company details (optional and only when trn present)
)
extends OldBusinessDetails
//  def getNinoOrTrn: String = nino.orElse(trn).getOrElse(throw new RuntimeException("Sole trader missing nino and trn"))

object OldSoleTraderDetails:
  given Format[OldSoleTraderDetails] = Json.format[OldSoleTraderDetails]

final case class OldPartnershipDetails(
  safeId: SafeId,
  businessType: BusinessType,
  companyProfile: Option[CompanyProfile],
  postcode: String
)
extends OldBusinessDetails

object OldPartnershipDetails:
  given Format[OldPartnershipDetails] = Json.format[OldPartnershipDetails]
