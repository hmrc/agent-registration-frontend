/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.agentregistrationfrontend.model.addresslookup.Country
import uk.gov.hmrc.agentregistrationfrontend.model.addresslookup.GetConfirmedAddressResponse
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication.sections.*

trait TdAllSections
extends TdSectionContactDetails,
  TdSectionAgentDetails,
  TdUpload,
  TdSectionAmls:
  this: TdBase =>

  def getConfirmedAddressResponse: GetConfirmedAddressResponse = GetConfirmedAddressResponse(
    lines = Seq("New Line 1", "New Line 2"),
    postcode = Some("CD3 4EF"),
    country = Country(
      code = "GB",
      name = Some("United Kingdom")
    )
  )
