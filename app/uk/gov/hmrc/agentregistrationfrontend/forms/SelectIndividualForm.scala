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

package uk.gov.hmrc.agentregistrationfrontend.forms

import play.api.data.Form
import play.api.data.Forms
import play.api.data.Mapping
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.forms.mappings.Mappings

object SelectIndividualForm:

  val key = "selectIndividual"

  def form(individuals: List[IndividualProvidedDetails]): Form[IndividualProvidedDetails] =
    val mapping: Mapping[IndividualProvidedDetails] = Mappings.textFromOptions(
      formMessageKey = key,
      options = individuals.map(_.individualProvidedDetailsId.value)
    ).transform[IndividualProvidedDetails](
      individualProvidedDetailsId =>
        individuals
          .find(_.individualProvidedDetailsId.value === individualProvidedDetailsId)
          .getOrElse(throw new NoSuchElementException(
            s"Unable to find individual with id $individualProvidedDetailsId in the list of individuals yet to provide details"
          )),
      individual => individual.individualProvidedDetailsId.value
    )
    Form(Forms.single(key -> mapping))
