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
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfRequiredKeyIndividuals
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore

sealed trait ApplicationIndividualsListTest:

  val numberOfKeyIndividuals: NumberOfRequiredKeyIndividuals
  val providedDetailsState: ProvidedDetailsState

object ApplicationIndividualsListTest:

  final case class FiveOrLessPreCreated(number: Int)
  extends ApplicationIndividualsListTest:

    override val providedDetailsState: ProvidedDetailsState = ProvidedDetailsState.Precreated
    override val numberOfKeyIndividuals: NumberOfRequiredKeyIndividuals = FiveOrLess(number)

  final case class FiveOrLessFinished(number: Int)
  extends ApplicationIndividualsListTest:

    override val providedDetailsState: ProvidedDetailsState = ProvidedDetailsState.Finished
    override val numberOfKeyIndividuals: NumberOfRequiredKeyIndividuals = FiveOrLess(number)

  final case class SixOrMorePreCreated(number: Int)
  extends ApplicationIndividualsListTest:

    override val providedDetailsState: ProvidedDetailsState = ProvidedDetailsState.Precreated
    override val numberOfKeyIndividuals: NumberOfRequiredKeyIndividuals = SixOrMore(number)
