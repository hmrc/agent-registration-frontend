/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.testonly.model

import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData

sealed trait ApplicationIndividualsListTest:

  // Can be either number of required individuals or number of company house individuals
  val numberOfIndividuals: NumberOfIndividuals
  val individualProvidedDetails: IndividualProvidedDetails

object ApplicationIndividualsListTest:

  sealed trait RequiredKeyIndividuals
  extends ApplicationIndividualsListTest:

    override val numberOfIndividuals: NumberOfRequiredKeyIndividuals
    override val individualProvidedDetails: IndividualProvidedDetails

  object RequiredKeyIndividuals:

    object TwoPreCreated
    extends RequiredKeyIndividuals:

      override val numberOfIndividuals: NumberOfRequiredKeyIndividuals = FiveOrLess(2)
      override val individualProvidedDetails: IndividualProvidedDetails = TestOnlyData.providedDetails.unclaimed

    object TwoFinished
    extends RequiredKeyIndividuals:

      override val numberOfIndividuals: NumberOfRequiredKeyIndividuals = FiveOrLess(2)
      override val individualProvidedDetails: IndividualProvidedDetails = TestOnlyData.providedDetails.afterProvidedDetailsConfirmed

    object SixPreCreated
    extends RequiredKeyIndividuals:

      override val numberOfIndividuals: NumberOfRequiredKeyIndividuals = SixOrMore(6)
      override val individualProvidedDetails: IndividualProvidedDetails = TestOnlyData.providedDetails.unclaimed

  sealed trait CompaniesHouseOfficers
  extends ApplicationIndividualsListTest:

    override val numberOfIndividuals: NumberOfCompaniesHouseOfficers
    override val individualProvidedDetails: IndividualProvidedDetails

  object CompaniesHouseOfficers:

    object TwoPreCreatedOfficersCorrect
    extends CompaniesHouseOfficers:

      override val numberOfIndividuals: NumberOfCompaniesHouseOfficers = FiveOrLessOfficers(2, true)
      override val individualProvidedDetails: IndividualProvidedDetails = TestOnlyData.providedDetails.unclaimed

    object TwoFinishedOfficersCorrect
    extends CompaniesHouseOfficers:

      override val numberOfIndividuals: NumberOfCompaniesHouseOfficers = FiveOrLessOfficers(2, true)
      override val individualProvidedDetails: IndividualProvidedDetails = TestOnlyData.providedDetails.afterProvidedDetailsConfirmed

    object SixPreCreatedOfficersAllResponsible
    extends CompaniesHouseOfficers:

      override val numberOfIndividuals: NumberOfCompaniesHouseOfficers = SixOrMoreOfficers(6, 6)
      override val individualProvidedDetails: IndividualProvidedDetails = TestOnlyData.providedDetails.unclaimed
