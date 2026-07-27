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

import play.api.mvc.PathBindable
import play.api.mvc.QueryStringBindable

/** Filename of a risking results file uploaded/retrieved via the Minerva simulator's test-only SDES stub, eg. `test-only-entity-XARN1234567`.
  */
final case class RiskingResultsFilename(value: String)

object RiskingResultsFilename:

  given pathBindable: PathBindable[RiskingResultsFilename] = PathBindable.bindableString.transform(RiskingResultsFilename(_), _.value)
  given queryStringBindable: QueryStringBindable[RiskingResultsFilename] = QueryStringBindable.bindableString.transform(RiskingResultsFilename(_), _.value)
