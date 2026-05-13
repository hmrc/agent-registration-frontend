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

package uk.gov.hmrc.agentregistrationfrontend.forms.individual

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import play.api.data.Mapping
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.ErrorKeys

object IndividualNameSearchForm:

  val key: String = "individualNameSearch"
  private val nameRegex = "^[a-zA-Z\\-' ]+$"

  private def canonicalise(s: String): String = s.trim.replaceAll("\\s+", " ")

  def form(
    individuals: List[IndividualProvidedDetails],
    applicantName: String
  ): Form[IndividualProvidedDetails] =
    val existsConstraint: Constraint[String] =
      Constraint[String]("constraint.exists") { in =>
        if (individuals.exists(_.individualName.value === in))
          Valid
        else
          Invalid(s"$key.error.nameNotFound", applicantName)
      }

    val mappingToProvided: Mapping[IndividualProvidedDetails] = mapping(
      key -> text
        .transform[String](_.trim, identity)
        .verifying(ErrorKeys.requiredFieldErrorMessage(key), _.nonEmpty)
        .verifying(ErrorKeys.invalidInputErrorMessage(key), _.matches(nameRegex))
        .verifying(ErrorKeys.inputTooLongErrorMessage(key), _.length <= 100)
        .verifying(existsConstraint)
    )(individualName =>
      IndividualName(canonicalise(individualName))
    )((individualName: IndividualName) =>
      Some(individualName.value)
    )
      .transform[IndividualProvidedDetails](
        (name: IndividualName) =>
          individuals.find(_.individualName.value === name.value).getOrElse(
            throw new RuntimeException(
              s"[IndividualNameSearchForm] This should not happen, the constraint should have matched the name to an individual already."
            )
          ),
        (ipd: IndividualProvidedDetails) => ipd.individualName
      )

    Form(mappingToProvided)
