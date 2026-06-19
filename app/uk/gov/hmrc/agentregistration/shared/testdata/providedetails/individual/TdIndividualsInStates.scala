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

package uk.gov.hmrc.agentregistration.shared.testdata.providedetails.individual

import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement.Agreed
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.AccessConfirmed
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Finished
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Precreated
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Started
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.risking.submitforrisking.IndividualData
import uk.gov.hmrc.agentregistration.shared.testdata.TdApplicationIdentifiers

object TdIndividualsInStates:
  def make(
    _seed: String,
    _agentApplicationId: AgentApplicationId
  ): TdIndividualsInStates =
    new TdIndividualsInStates:
      override def seed: String = _seed
      override def agentApplicationId: AgentApplicationId = _agentApplicationId

trait TdIndividualsInStates:

  def seed: String
  def agentApplicationId: AgentApplicationId

  def invididualInStates1: TdIndividualInStates = TdIndividualInStates.make(seed + "01", agentApplicationId)
  def invididualInStates2: TdIndividualInStates = TdIndividualInStates.make(seed + "02", agentApplicationId)
  def invididualInStates3: TdIndividualInStates = TdIndividualInStates.make(seed + "03", agentApplicationId)
  def invididualInStates4: TdIndividualInStates = TdIndividualInStates.make(seed + "04", agentApplicationId)
  def invididualInStates5: TdIndividualInStates = TdIndividualInStates.make(seed + "05", agentApplicationId)
  def invididualInStates6: TdIndividualInStates = TdIndividualInStates.make(seed + "06", agentApplicationId)
