/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class BusinessTypeSpec extends AnyWordSpecLike with Matchers :

    BusinessType.values.foreach(value =>
      s"BusinessType.fromName should read $value" in :
        BusinessType.fromName(value.toString) shouldBe Some(value)
    )

    "BusinessType.fromName should fail to read an unknown value" in :
      BusinessType.fromName("invalid") shouldBe None

    "BusinessType.names should return all business type names" in :
      BusinessType.names should contain allOf (
        BusinessType.SoleTrader.name,
        BusinessType.LimitedCompany.name,
        BusinessType.GeneralPartnership.name,
        BusinessType.LimitedLiabilityPartnership.name
      )

    "BusinessType.all should return all business types" in :
      BusinessType.all should contain allOf (
        BusinessType.SoleTrader,
        BusinessType.LimitedCompany,
        BusinessType.GeneralPartnership,
        BusinessType.LimitedLiabilityPartnership
      )
