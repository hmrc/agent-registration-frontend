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

package uk.gov.hmrc.agentregistration.shared.testdata

import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.TdAgentApplicationLlpInStates
import uk.gov.hmrc.agentregistration.shared.testdata.providedetails.individual.TdIndividualsInStates

object TdApplicationFixture:
  def make(_seed: String) =
    new TdApplicationFixture:
      override def seed: String = _seed

trait TdApplicationFixture:

  def seed: String
  val applicationLlpInStates: TdAgentApplicationLlpInStates = agentapplication.TdAgentApplicationLlpInStates.make(seed)

  val tdIndividualsInStates: TdIndividualsInStates = TdIndividualsInStates.make(
    _seed = seed,
    _agentApplicationId = applicationLlpInStates.tdApplicationIdentifiers.agentApplicationId
  )
