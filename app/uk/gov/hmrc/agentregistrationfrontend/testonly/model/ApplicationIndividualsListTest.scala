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

import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfCompaniesHouseOfficers
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfIndividuals
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfRequiredKeyIndividuals
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers

sealed trait ApplicationIndividualsListTest:

  // Can be either number of required individuals or number of company house individuals
  val numberOfIndividuals: NumberOfIndividuals
  val providedDetailsState: ProvidedDetailsState

object ApplicationIndividualsListTest:

  sealed trait RequiredKeyIndividuals
  extends ApplicationIndividualsListTest:

    override val numberOfIndividuals: NumberOfRequiredKeyIndividuals
    override val providedDetailsState: ProvidedDetailsState

  object RequiredKeyIndividuals:

    object TwoPreCreated
    extends RequiredKeyIndividuals:

      override val providedDetailsState: ProvidedDetailsState = ProvidedDetailsState.Precreated
      override val numberOfIndividuals: NumberOfRequiredKeyIndividuals = FiveOrLess(2)

    object TwoFinished
    extends RequiredKeyIndividuals:

      override val providedDetailsState: ProvidedDetailsState = ProvidedDetailsState.Finished
      override val numberOfIndividuals: NumberOfRequiredKeyIndividuals = FiveOrLess(2)

    object SixPreCreated
    extends RequiredKeyIndividuals:

      override val providedDetailsState: ProvidedDetailsState = ProvidedDetailsState.Precreated
      override val numberOfIndividuals: NumberOfRequiredKeyIndividuals = SixOrMore(6)

  sealed trait CompaniesHouseOfficers
  extends ApplicationIndividualsListTest:

    override val numberOfIndividuals: NumberOfCompaniesHouseOfficers
    override val providedDetailsState: ProvidedDetailsState

  object CompaniesHouseOfficers:

    object TwoPreCreatedOfficersCorrect
    extends CompaniesHouseOfficers:

      override val providedDetailsState: ProvidedDetailsState = ProvidedDetailsState.Precreated
      override val numberOfIndividuals: NumberOfCompaniesHouseOfficers = FiveOrLessOfficers(2, true)

    object TwoFinishedOfficersCorrect
    extends CompaniesHouseOfficers:

      override val providedDetailsState: ProvidedDetailsState = ProvidedDetailsState.Finished
      override val numberOfIndividuals: NumberOfCompaniesHouseOfficers = FiveOrLessOfficers(2, true)

    object SixPreCreatedOfficersAllResponsible
    extends CompaniesHouseOfficers:

      override val providedDetailsState: ProvidedDetailsState = ProvidedDetailsState.Precreated
      override val numberOfIndividuals: NumberOfCompaniesHouseOfficers = SixOrMoreOfficers(6, 6)
