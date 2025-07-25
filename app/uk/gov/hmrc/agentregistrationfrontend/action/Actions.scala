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

import play.api.mvc.*

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Actions @Inject() (
  actionBuilder: DefaultActionBuilder,
  authenticatedAction: AuthenticatedAction,
  authorisedUtrAction: AuthorisedUtrAction,
  getApplicationActionRefiner: GetApplicationActionRefiner,
  ensureApplication: EnsureApplication
):

  val default: ActionBuilder[Request, AnyContent] = actionBuilder

  val authorisedUtr: ActionBuilder[AuthorisedUtrRequest, AnyContent] = default
    .andThen(authenticatedAction)
    .andThen(authorisedUtrAction)

  val getApplicationInProgress: ActionBuilder[ApplicationRequest, AnyContent] = authorisedUtr
    .andThen(getApplicationActionRefiner)
    .andThen(ensureApplication.ensureApplication(
      predicate = _.isInProgress,
      redirectF = _ => uk.gov.hmrc.agentregistrationfrontend.controllers.routes.ApplicationController.applicationSubmitted,
      hintWhyRedirecting = "The application is in the final state"
    ))

  val getApplicationSubmitted: ActionBuilder[ApplicationRequest, AnyContent] = authorisedUtr
    .andThen(getApplicationActionRefiner)
    .andThen(ensureApplication.ensureApplication(
      predicate = _.hasFinished,
      redirectF = _ => uk.gov.hmrc.agentregistrationfrontend.controllers.routes.ApplicationController.landing,
      hintWhyRedirecting = "The application is not in the final state"
    ))
