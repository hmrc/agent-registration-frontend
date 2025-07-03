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

package uk.gov.hmrc.agentregistrationfrontend.model.application

import enumeratum.{Enum, EnumEntry}
import play.api.libs.json.Format
import uk.gov.hmrc.agentregistrationfrontend.util.EnumFormat

import scala.collection.immutable

sealed trait ApplicationState extends EnumEntry

object ApplicationState {
  implicit val format: Format[ApplicationState] = EnumFormat(ApplicationStates)
}

object ApplicationStates extends Enum[ApplicationState] {
  case object InProgress extends ApplicationState
  case object Submitted extends ApplicationState

  override def values: immutable.IndexedSeq[ApplicationState] = findValues
}