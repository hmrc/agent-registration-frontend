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

import uk.gov.hmrc.agentregistration.shared.Crn
import uk.gov.hmrc.agentregistration.shared.testdata.TestOnlyData

/** Test Data (Td) user for TestOnly endpoints
  */
trait TdTestOnly
extends TestOnlyData
with TdGrsJourneyData

object TdTestOnly
extends TdTestOnly:

  object llp:

    val twoChOfficers: TdTestOnly =
      new TdTestOnly:
        val crnTwoChOfficers = Crn("22222222")
        override def crn: Crn = crnTwoChOfficers

    val sixChOfficers: TdTestOnly =
      new TdTestOnly:
        val crnSixChOfficers = Crn("22222226")
        override def crn: Crn = crnSixChOfficers
