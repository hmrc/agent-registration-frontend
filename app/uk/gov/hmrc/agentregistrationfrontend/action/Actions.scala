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
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.*
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedAction
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedWithIdentifiersAction
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedWithIdentifiersRequest
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.EnrichWithAgentApplicationAction
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.IndividualProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.IndividualProvideDetailsWithApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.ProvideDetailsAction
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.SubmissionHelper
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.UniqueTuple.AbsentIn
import uk.gov.hmrc.agentregistrationfrontend.util.UniqueTuple.PresentIn
import uk.gov.hmrc.agentregistrationfrontend.util.Errors.*
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class Actions @Inject() (
  actionBuilder: DefaultActionBuilder,
  authorisedAction: AuthorisedAction,
  authorisedActionRefiner: AuthorisedActionRefiner,
  agentApplicationAction: AgentApplicationAction,
  individualAuthorisedAction: IndividualAuthorisedAction,
  individualAuthorisedWithIdentifiersAction: IndividualAuthorisedWithIdentifiersAction,
  provideDetailsAction: ProvideDetailsAction,
  enrichWithAgentApplicationAction: EnrichWithAgentApplicationAction,
  agentApplicationService: AgentApplicationService,
  businessPartnerRecordService: BusinessPartnerRecordService
)(using ExecutionContext)
extends RequestAwareLogging:

  export ActionsHelper.*

  extension [Data <: Tuple](ab: ActionBuilder4[Data])

    inline def getBusinessPartnerRecord(using
      AgentApplication PresentIn Data,
      BusinessPartnerRecordResponse AbsentIn Data
    ): ActionBuilder4[BusinessPartnerRecordResponse *: Data] = ab.refine4:
      implicit request =>
        businessPartnerRecordService
          .getBusinessPartnerRecord(request.get[AgentApplication].getUtr)
          .map(_.getOrThrowExpectedDataMissing(s"Business Partner Record for UTR ${request.get[AgentApplication].getUtr.value}"))
          .map(request.add)

    inline def getMaybeBusinessPartnerRecord(using
      AgentApplication PresentIn Data,
      Option[BusinessPartnerRecordResponse] AbsentIn Data
    ): ActionBuilder4[Option[BusinessPartnerRecordResponse] *: Data] = ab.refine4:
      implicit request =>
        businessPartnerRecordService
          .getBusinessPartnerRecord(request.get[AgentApplication].getUtr)
          .map(request.add)

  val deleteMeAction2: ActionBuilder[DefaultRequest, AnyContent] = actionBuilder
    .refine2(request => RequestWithData.empty(request))

  val action: ActionBuilder4[EmptyTuple] = deleteMeAction2

  val deleteMeAction: ActionBuilder[Request, AnyContent] = actionBuilder

  object Applicant:

    val deleteMeAuthorised2: ActionBuilder[AuthorisedRequest2, AnyContent] = deleteMeAction2
      .refineAsync(authorisedActionRefiner.refine)

    val authorised4: ActionBuilder4[DataWithAuth] = action
      .refineAsync(authorisedActionRefiner.refine)

    val deleteMeAuthorised: ActionBuilder[AuthorisedRequest, AnyContent] = deleteMeAction
      .andThen(authorisedAction)

    val deleteMeGetApplication: ActionBuilder[AgentApplicationRequest, AnyContent] = deleteMeAuthorised
      .andThen(agentApplicationAction)

    val deleteMeGetApplication2Old: ActionBuilder[AgentApplicationRequest2, AnyContent] = deleteMeAuthorised2
      .refineAsync(agentApplicationAction.refineRequest)

    val deleteMeGetApplication2: ActionBuilder[AgentApplicationRequest2, AnyContent] = deleteMeAuthorised2
      .refine2:
        implicit request: (AuthorisedRequest2[AnyContent]) =>
          agentApplicationService
            .find2()
            .map[Result | AgentApplicationRequest2[AnyContent]]:
              case Some(agentApplication) => request.add(agentApplication)
              case None =>
                val redirect = AppRoutes.apply.AgentApplicationController.startRegistration
                logger.error(s"[Unexpected State] No agent application found for authenticated user ${request.get[InternalUserId].value}. Redirecting to startRegistration page ($redirect)")
                Redirect(redirect)

    val getApplication: ActionBuilder4[DataWithApplication] = authorised4
      .refine4:
        implicit request: RequestWithData4[DataWithAuth] =>
          agentApplicationService
            .find2()
            .map[Result | AgentApplicationRequest2[AnyContent]]:
              case Some(agentApplication) => request.add(agentApplication)
              case None =>
                val redirect = AppRoutes.apply.AgentApplicationController.startRegistration
                logger.error(s"[Unexpected State] No agent application found for authenticated user ${request.get[InternalUserId].value}. Redirecting to startRegistration page ($redirect)")
                Redirect(redirect)

    val deleteMeGetApplicationInProgress: ActionBuilder[AgentApplicationRequest, AnyContent] = deleteMeGetApplication
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

    val deleteMeGetApplicationInProgress2: ActionBuilder[AgentApplicationRequest2, AnyContent] = deleteMeGetApplication2
      .ensure(
        condition = _.get[AgentApplication].isInProgress,
        resultWhenConditionNotMet =
          implicit request =>
            // TODO: this is a temporary solution and should be revisited once we have full journey implemented
            val call = AppRoutes.apply.AgentApplicationController.applicationSubmitted
            logger.warn(
              s"The application is not in the final state" +
                s" (current application state: ${request.get[AgentApplication].applicationState.toString}), " +
                s"redirecting to [${call.url}]. User might have used back or history to get to ${request.path} from previous page."
            )
            Redirect(call.url)
      )

    def getApplicationInProgress: ActionBuilder4[DataWithApplication] = getApplication
      .ensure(
        condition = _.get[AgentApplication].isInProgress,
        resultWhenConditionNotMet =
          implicit request =>
            // TODO: this is a temporary solution and should be revisited once we have full journey implemented
            val call = AppRoutes.apply.AgentApplicationController.applicationSubmitted
            logger.warn(
              s"The application is not in the final state" +
                s" (current application state: ${request.get[AgentApplication].applicationState.toString}), " +
                s"redirecting to [${call.url}]. User might have used back or history to get to ${request.path} from previous page."
            )
            Redirect(call.url)
      )

    val deleteMeGetApplicationSubmitted: ActionBuilder[AgentApplicationRequest, AnyContent] = deleteMeGetApplication
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

    val getApplicationSubmitted4: ActionBuilder4[DataWithApplication] = getApplication
      .ensure(
        condition = _.agentApplication.hasFinished,
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

    val deleteMeGetApplicationSubmitted2: ActionBuilder[AgentApplicationRequest2, AnyContent] = deleteMeGetApplication2
      .ensure(
        condition = (r: AgentApplicationRequest2[?]) => r.get[AgentApplication].hasFinished,
        resultWhenConditionNotMet =
          implicit request =>
            // TODO: this is a temporary solution and should be revisited once we have full journey implemented
            val call = AppRoutes.apply.AgentApplicationController.landing // or task list
            logger.warn(
              s"The application is not in the final state" +
                s" (current application state: ${request.get[AgentApplication].applicationState.toString}), " +
                s"redirecting to [${call.url}]. User might have used back or history to get to ${request.path} from previous page."
            )
            Redirect(call.url)
      )

  object Individual:

    val authorised: ActionBuilder[IndividualAuthorisedRequest, AnyContent] = deleteMeAction
      .andThen(individualAuthorisedAction)

    val authorisedWithIdentifiers: ActionBuilder[IndividualAuthorisedWithIdentifiersRequest, AnyContent] = deleteMeAction
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
