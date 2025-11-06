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

import play.api.data.Form
import play.api.data.FormBinding
import play.api.mvc.*
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results.Redirect
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedAction
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.MemberProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.ProvideDetailsAction
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.SubmissionHelper

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.util.chaining.scalaUtilChainingOps

@Singleton
class Actions @Inject() (
  actionBuilder: DefaultActionBuilder,
  authorisedAction: AuthorisedAction,
  agentApplicationAction: AgentApplicationAction,
  individualAuthorisedAction: IndividualAuthorisedAction,
  provideDetailsAction: ProvideDetailsAction
)(using ExecutionContext)
extends RequestAwareLogging:

  export ActionsHelper.*

  val action: ActionBuilder[Request, AnyContent] = actionBuilder

  val authorised: ActionBuilder[AuthorisedRequest, AnyContent] = action
    .andThen(authorisedAction)

  val getApplicationInProgress: ActionBuilder[AgentApplicationRequest, AnyContent] = authorised
    .andThen(agentApplicationAction)
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

  val getApplicationSubmitted: ActionBuilder[AgentApplicationRequest, AnyContent] = authorised
    .andThen(agentApplicationAction)
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

  extension (ab: ActionBuilder[AgentApplicationRequest, AnyContent])(using ec: ExecutionContext)

    def ensureValidFormAndRedirectIfSaveForLater[T](
      form: AgentApplicationRequest[AnyContent] => Form[T],
      viewToServeWhenFormHasErrors: AgentApplicationRequest[AnyContent] => Form[T] => HtmlFormat.Appendable
    )(using
      fb: FormBinding,
      merge: MergeFormValue[AgentApplicationRequest[AnyContent], T]
    ): ActionBuilder[[X] =>> AgentApplicationRequest[X] & FormValue[T], AnyContent] = ab
      .ensureValidFormGeneric[T](
        form,
        (r: AgentApplicationRequest[AnyContent]) =>
          (f: Form[T]) =>
            viewToServeWhenFormHasErrors(r)(f)
              .pipe(BadRequest.apply)
              .pipe(SubmissionHelper.redirectIfSaveForLater(r, _))
      )

    def ensureValidFormAndRedirectIfSaveForLater[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: AgentApplicationRequest[AnyContent] => Form[T] => HtmlFormat.Appendable
    )(using
      fb: FormBinding,
      merge: MergeFormValue[AgentApplicationRequest[AnyContent], T]
    ): ActionBuilder[[X] =>> AgentApplicationRequest[X] & FormValue[T], AnyContent] = ab.ensureValidFormAndRedirectIfSaveForLater(
      _ => form,
      viewToServeWhenFormHasErrors
    )

  extension (a: Action[AnyContent])
    /** Modifies the action result to handle "Save and Come Back Later" functionality. If the form submission contains a "Save and Come Back Later" action,
      * redirects to the Save and Come Back Later page. Otherwise, returns the original result unchanged.
      */
    def redirectIfSaveForLater: Action[AnyContent] = a.mapResult(request =>
      originalResult =>
        if SubmissionHelper.getSubmitAction(request).isSaveAndComeBackLater then Redirect(AppRoutes.apply.SaveForLaterController.show)
        else originalResult
    )

  val authorisedIndividual: ActionBuilder[IndividualAuthorisedRequest, AnyContent] = action
    .andThen(individualAuthorisedAction)

  val getProvideDetailsInProgress: ActionBuilder[MemberProvideDetailsRequest, AnyContent] = authorisedIndividual
    .andThen(provideDetailsAction)
    .ensure(
      condition = _.memberProvidedDetails.isInProgress,
      resultWhenConditionNotMet =
        implicit request =>
          // TODO: TBC - where to redirect
          val call = AppRoutes.apply.AgentApplicationController.genericExitPage
          logger.warn(
            s"The provided details are in the final state" +
              s" (current provided details: ${request.memberProvidedDetails.providedDetailsState.toString}), " +
              s"redirecting to [${call.url}]."
          )
          Redirect(call.url)
    )

  val getSubmitedDetailsInProgress: ActionBuilder[MemberProvideDetailsRequest, AnyContent] = authorisedIndividual
    .andThen(provideDetailsAction)
    .ensure(
      condition = _.memberProvidedDetails.hasFinished,
      resultWhenConditionNotMet =
        implicit request =>
          // TODO: TBC - where to redirect
          val call = AppRoutes.apply.AgentApplicationController.genericExitPage
          logger.warn(
            s"The provided details are in the final state" +
              s" (current provided details: ${request.memberProvidedDetails.providedDetailsState.toString}), " +
              s"redirecting to [${call.url}]."
          )
          Redirect(call.url)
    )
