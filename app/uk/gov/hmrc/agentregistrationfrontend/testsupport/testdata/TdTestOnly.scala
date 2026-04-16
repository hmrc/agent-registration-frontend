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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
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

  object limitedCompany:

    val twoChOfficers: TdTestOnly =
      new TdTestOnly:
        val crnTwoChOfficers = Crn("11111111")

        override def crn: Crn = crnTwoChOfficers

    val sixChOfficers: TdTestOnly =
      new TdTestOnly:
        val crnSixChOfficers = Crn("11111116")

        override def crn: Crn = crnSixChOfficers

  object limitedPartnership:

    val twoChOfficers: TdTestOnly =
      new TdTestOnly:
        val crnTwoChOfficers = Crn("33333333")

        override def crn: Crn = crnTwoChOfficers

    val sixChOfficers: TdTestOnly =
      new TdTestOnly:
        val crnSixChOfficers = Crn("33333336")

        override def crn: Crn = crnSixChOfficers

  object scottishLimitedPartnership:

    val twoChOfficers: TdTestOnly =
      new TdTestOnly:
        val crnTwoChOfficers = Crn("44444444")

        override def crn: Crn = crnTwoChOfficers

    val sixChOfficers: TdTestOnly =
      new TdTestOnly:
        val crnSixChOfficers = Crn("44444446")

        override def crn: Crn = crnSixChOfficers

  object additionalIndividuals:

    val secondIndividual: TdTestOnly =
      new TdTestOnly:
        val secondIndividualProvidedDetailsId = IndividualProvidedDetailsId("individual-provided-details-id-22345")
        val secondIndividualName = IndividualName("Second Test Name")
        override def individualProvidedDetailsId: IndividualProvidedDetailsId = secondIndividualProvidedDetailsId
        override def individualName: IndividualName = secondIndividualName

    val thirdIndividual: TdTestOnly =
      new TdTestOnly:
        val thirdIndividualProvidedDetailsId = IndividualProvidedDetailsId("individual-provided-details-id-32345")
        val thirdIndividualName = IndividualName("Third Test Name")
        override def individualProvidedDetailsId: IndividualProvidedDetailsId = thirdIndividualProvidedDetailsId
        override def individualName: IndividualName = thirdIndividualName
