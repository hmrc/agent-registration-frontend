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
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Actions @Inject() (
  actionBuilder: DefaultActionBuilder,
  authorisedAction: AuthorisedAction,
  agentApplicationAction: AgentApplicationAction,
  requestEnsurer: RequestEnsurer
)
extends RequestAwareLogging:

  export requestEnsurer.*

  val action: ActionBuilder[Request, AnyContent] = actionBuilder

  val authorised: ActionBuilder[AuthorisedRequest, AnyContent] = action
    .andThen(authorisedAction)

  val getApplicationInProgress: ActionBuilder[AgentApplicationRequest, AnyContent] = authorised
    .andThen(agentApplicationAction)
    .ensure(
      condition = _.agentApplication.isInProgress,
      resultWhenConditionNotMet =
        request =>
          // TODO: this is a temporary solution and should be revisited once we have full journey implemented
          val call = uk.gov.hmrc.agentregistrationfrontend.controllers.routes.AgentApplicationController.applicationSubmitted
          logger.warn(
            s"The application is not in the final state" +
              s" (current application state: ${request.agentApplication.applicationState.toString}), " +
              s"redirecting to [${call.url}]. User might have used back or history to get to ${request.path} from previous page."
          )(using request)
          Redirect(call.url)
    )

  val getApplicationSubmitted: ActionBuilder[AgentApplicationRequest, AnyContent] = authorised
    .andThen(agentApplicationAction)
    .ensure(
      condition = (r: AgentApplicationRequest[?]) => r.agentApplication.hasFinished,
      resultWhenConditionNotMet =
        request =>
          // TODO: this is a temporary solution and should be revisited once we have full journey implemented
          val call = uk.gov.hmrc.agentregistrationfrontend.controllers.routes.AgentApplicationController.landing // or task list
          logger.warn(
            s"The application is not in the final state" +
              s" (current application state: ${request.agentApplication.applicationState.toString}), " +
              s"redirecting to [${call.url}]. User might have used back or history to get to ${request.path} from previous page."
          )(using request)
          Redirect(call.url)
    )
