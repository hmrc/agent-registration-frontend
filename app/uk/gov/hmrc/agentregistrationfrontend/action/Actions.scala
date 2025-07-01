/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.action

import play.api.mvc._
import uk.gov.hmrc.agentregistrationfrontend.util.SafeEquals.EqualsOps

import javax.inject.{Inject, Singleton}

@Singleton
class Actions @Inject() (
    actionBuilder:                          DefaultActionBuilder,
    authenticatedAction: AuthenticatedAction,
    authorisedUtrAction: AuthorisedUtrAction,
    getJourneyActionRefiner:                GetJourneyActionRefiner,
    ensureJourney:                          EnsureJourney
) {

  val default: ActionBuilder[Request, AnyContent] = actionBuilder

  val getJourneyInProgress: ActionBuilder[JourneyRequest, AnyContent] =
    default
    .andThen(authenticatedAction)
    .andThen(authorisedUtrAction)
    .andThen(getJourneyActionRefiner)
}
