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

package uk.gov.hmrc.agentregistrationfrontend.shared.action

import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentregistrationfrontend.individual.action.EnrichWithAgentApplicationAction
import uk.gov.hmrc.agentregistrationfrontend.individual.action.IndividualAuthorisedAction
import uk.gov.hmrc.agentregistrationfrontend.individual.action.IndividualAuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.individual.action.IndividualAuthorisedWithIdentifiersAction
import uk.gov.hmrc.agentregistrationfrontend.individual.action.IndividualAuthorisedWithIdentifiersRequest
import uk.gov.hmrc.agentregistrationfrontend.individual.action.IndividualProvideDetailsAction
import uk.gov.hmrc.agentregistrationfrontend.individual.action.IndividualProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.individual.action.IndividualProvideDetailsWithApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.applicant.action.AgentApplicationAction
import uk.gov.hmrc.agentregistrationfrontend.applicant.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.applicant.action.AuthorisedAction
import uk.gov.hmrc.agentregistrationfrontend.applicant.action.AuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.SubmissionHelper
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class Actions @Inject() (
  actionBuilder: DefaultActionBuilder,
  authorisedAction: AuthorisedAction,
  agentApplicationAction: AgentApplicationAction,
  individualAuthorisedAction: IndividualAuthorisedAction,
  individualAuthorisedWithIdentifiersAction: IndividualAuthorisedWithIdentifiersAction,
  provideDetailsAction: IndividualProvideDetailsAction,
  enrichWithAgentApplicationAction: EnrichWithAgentApplicationAction
)(using ExecutionContext)
extends RequestAwareLogging:

  export ActionsHelper.*

  val action: ActionBuilder[Request, AnyContent] = actionBuilder

  object Applicant:

    val authorised: ActionBuilder[AuthorisedRequest, AnyContent] = action
      .andThen(authorisedAction)

    val getApplication: ActionBuilder[AgentApplicationRequest, AnyContent] = authorised
      .andThen(agentApplicationAction)

    val getApplicationInProgress: ActionBuilder[AgentApplicationRequest, AnyContent] = getApplication
      .ensure(
        condition = _.agentApplication.isInProgress,
        resultWhenConditionNotMet =
          implicit request =>
            // TODO: this is a temporary solution and should be revisited once we have full journey implemented
            val call = AppRoutes.apply.AgentApplicationController.applicationSubmitted
            logger.warn(
              s"The application is not in the final state" +
                s" (current application state: ${request.agentApplication.applicationState.toString}), " +
                s"redirecting to [${call.url}]. User might have used back or history to get to ${request.path} from previous page."
            )
            Redirect(call.url)
      )

    val getApplicationSubmitted: ActionBuilder[AgentApplicationRequest, AnyContent] = getApplication
      .ensure(
        condition = (r: AgentApplicationRequest[?]) => r.agentApplication.hasFinished,
        resultWhenConditionNotMet =
          implicit request =>
            // TODO: this is a temporary solution and should be revisited once we have full journey implemented
            val call = AppRoutes.apply.AgentApplicationController.landing // or task list
            logger.warn(
              s"The application is not in the final state" +
                s" (current application state: ${request.agentApplication.applicationState.toString}), " +
                s"redirecting to [${call.url}]. User might have used back or history to get to ${request.path} from previous page."
            )
            Redirect(call.url)
      )

  object Individual:

    val authorised: ActionBuilder[IndividualAuthorisedRequest, AnyContent] = action
      .andThen(individualAuthorisedAction)

    val authorisedWithIdentifiers: ActionBuilder[IndividualAuthorisedWithIdentifiersRequest, AnyContent] = action
      .andThen(individualAuthorisedWithIdentifiersAction)

    val getProvidedDetails: ActionBuilder[IndividualProvideDetailsRequest, AnyContent] = authorised
      .andThen(provideDetailsAction)
    val getProvideDetailsInProgress: ActionBuilder[IndividualProvideDetailsRequest, AnyContent] = getProvidedDetails
      .ensure(
        condition = _.individualProvidedDetails.isInProgress,
        resultWhenConditionNotMet =
          implicit request =>
            val mpdConfirmationPage = AppRoutes.providedetails.IndividualConfirmationController.show
            logger.warn(
              s"The provided details have already been confirmed" +
                s" (current provided details: ${request.individualProvidedDetails.providedDetailsState.toString}), " +
                s"redirecting to [${mpdConfirmationPage.url}]."
            )
            Redirect(mpdConfirmationPage.url)
      )

    val getProvideDetailsWithApplicationInProgress: ActionBuilder[
      IndividualProvideDetailsWithApplicationRequest,
      AnyContent
    ] = getProvideDetailsInProgress.andThen(enrichWithAgentApplicationAction)

    val getSubmitedDetailsWithApplicationInProgress: ActionBuilder[IndividualProvideDetailsWithApplicationRequest, AnyContent] = getProvidedDetails
      .ensure(
        condition = _.individualProvidedDetails.hasFinished,
        resultWhenConditionNotMet =
          implicit request =>
            val mdpCyaPage = AppRoutes.providedetails.CheckYourAnswersController.show
            logger.warn(
              s"The provided details are not in the final state" +
                s" (current provided details: ${request.individualProvidedDetails.providedDetailsState.toString}), " +
                s"redirecting to [${mdpCyaPage.url}]."
            )
            Redirect(mdpCyaPage.url)
      ).andThen(enrichWithAgentApplicationAction)

  extension (a: Action[AnyContent])
    /** Modifies the action result to handle "Save and Come Back Later" functionality. If the form submission contains a "Save and Come Back Later" action,
      * redirects to the Save and Come Back Later page. Otherwise, returns the original result unchanged.
      */
    def redirectIfSaveForLater: Action[AnyContent] = a.mapResult(request =>
      originalResult =>
        if SubmissionHelper.getSubmitAction(request).isSaveAndComeBackLater then Redirect(AppRoutes.apply.SaveForLaterController.show)
        else originalResult
    )
