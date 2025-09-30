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

package uk.gov.hmrc.agentregistrationfrontend.model

import play.api.libs.json.Format
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*

enum SubmitAction:

  case SaveAndContinue
  case SaveAndComeBackLater

object SubmitAction:

  def fromSubmissionWithDefault(opt: Option[String]): SubmitAction =
    opt match {
      case Some("SaveAndContinue") => SaveAndContinue
      case Some("SaveAndComeBackLater") => SaveAndComeBackLater
      case _ => SaveAndContinue // default
    }

  extension (submitAction: SubmitAction)
    def isSaveAndComeBackLater: Boolean = submitAction === SubmitAction.SaveAndComeBackLater

  given Format[SubmitAction] = JsonFormatsFactory.makeEnumFormat[SubmitAction]
