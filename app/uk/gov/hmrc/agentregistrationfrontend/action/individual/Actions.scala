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

package uk.gov.hmrc.agentregistrationfrontend.action.individual

import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistrationfrontend.action.ActionsHelper
import uk.gov.hmrc.agentregistrationfrontend.action.RequestWithDataCt
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.*
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.IndividualProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.IndividualProvideDetailsWithApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.retrieve.Credentials

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

object Actions:

  export uk.gov.hmrc.agentregistrationfrontend.action.Actions.*

  type DataWithAuth = (InternalUserId, Credentials)
  type RequestWithAuth = RequestWithData[DataWithAuth]
  type RequestWithAuthCt[ContentType] = RequestWithDataCt[ContentType, DataWithAuth]

//  object Applicant:

@Singleton
class Actions @Inject() (
  defaultActionBuilder: DefaultActionBuilder,
  individualAuthorisedRefiner: IndividualAuthorisedRefiner,
  individualAuthorisedWithIdentifiersAction: IndividualAuthorisedWithIdentifiersAction,
  agentApplicationService: AgentApplicationService,
  businessPartnerRecordService: BusinessPartnerRecordService
)(using ExecutionContext)
extends RequestAwareLogging:

  export ActionsHelper.*
  export Actions.*

  // TODO move it to common actions
  private val action: ActionBuilderWithData[EmptyTuple] = defaultActionBuilder
    .refine2(request => RequestWithDataCt.empty(request))

  val authorisedNew: ActionBuilderWithData[DataWithAuth] = action
    .refineAsync(individualAuthorisedRefiner.refine)

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
