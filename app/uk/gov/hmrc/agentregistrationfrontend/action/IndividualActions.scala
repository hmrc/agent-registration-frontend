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
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistrationfrontend.action.individual.*
import uk.gov.hmrc.agentregistrationfrontend.action.individual.llp.EnrichWithAgentApplicationAction
import uk.gov.hmrc.agentregistrationfrontend.action.individual.llp.IndividualProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.action.individual.llp.IndividualProvideDetailsWithApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.action.individual.llp.ProvideDetailsAction
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.retrieve.Credentials

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

object IndividualActions:

  export uk.gov.hmrc.agentregistrationfrontend.action.Actions.*

  type DataWithAuth = (InternalUserId, Credentials)
  type RequestWithAuth = RequestWithData[DataWithAuth]
  type RequestWithAuthCt[ContentType] = RequestWithDataCt[ContentType, DataWithAuth]

  type DataWithAdditionalIdentifiers = Option[Nino] *: Option[SaUtr] *: DataWithAuth
  type RequestWithAdditionalIdentifiers = RequestWithData[DataWithAdditionalIdentifiers]
  type RequestWithAdditionalIdentifiersCt[ContentType] = RequestWithDataCt[ContentType, DataWithAdditionalIdentifiers]

  type DataWithIndividualProvidedDetails = IndividualProvidedDetailsToBeDeleted *: DataWithAuth
  type RequestWithIndividualProvidedDetailsToBeDeleted = RequestWithData[DataWithIndividualProvidedDetails]
  type RequestWithIndividualProvidedDetailsToBeDeletedCt[ContentType] = RequestWithDataCt[ContentType, DataWithIndividualProvidedDetails]

  type DataWithAgentApplication = AgentApplication *: DataWithIndividualProvidedDetails
  type RequestWithAgentApplication = RequestWithData[DataWithAgentApplication]
  type RequestWithAgentApplicationCt[ContentType] = RequestWithDataCt[ContentType, DataWithAgentApplication]

@Singleton
class IndividualActions @Inject() (
  defaultActionBuilder: DefaultActionBuilder,
  individualAuthorisedRefiner: IndividualAuthRefiner,
  individualAuthorisedAction: IndividualAuthorisedAction,
  individualAuthorisedWithIdentifiersAction: IndividualAuthorisedWithIdentifiersAction,
  individualProvideDetailsRefiner: IndividualProvideDetailsRefiner,
  provideDetailsAction: ProvideDetailsAction,
  enrichWithAgentApplicationAction: EnrichWithAgentApplicationAction,
  enricherAgentApplication: EnricherAgentApplication
)(using ExecutionContext)
extends RequestAwareLogging:

  export ActionBuilders.*
  export IndividualActions.*

  val action: ActionBuilderWithData[EmptyTuple] = defaultActionBuilder
    .refine(request => RequestWithDataCt.empty(request))

  val authorised: ActionBuilderWithData[DataWithAuth] = action
    .refineFutureEither(individualAuthorisedRefiner.refineIntoRequestWithAuth)

  val DELETEMEauthorised: ActionBuilder[IndividualAuthorisedRequest, AnyContent] = action
    .andThen(individualAuthorisedAction)

  val authorisedWithAdditionalIdentifiers: ActionBuilderWithData[DataWithAdditionalIdentifiers] = action
    .refineFutureEither(individualAuthorisedRefiner.refineIntoRequestWithAdditionalIdentifiers)

  val DELETEMEauthorisedWithIdentifiers: ActionBuilder[IndividualAuthorisedWithIdentifiersRequest, AnyContent] = action
    .andThen(individualAuthorisedWithIdentifiersAction)

  val getProvidedDetails: ActionBuilderWithData[DataWithIndividualProvidedDetails] = authorised
    .refineFutureEither:
      individualProvideDetailsRefiner.refineIntoRequestWithIndividualProvidedDetails

  val DELETEMEgetProvidedDetails: ActionBuilder[IndividualProvideDetailsRequest, AnyContent] = DELETEMEauthorised
    .andThen(provideDetailsAction)

  val getProvideDetailsInProgress: ActionBuilderWithData[DataWithIndividualProvidedDetails] = getProvidedDetails
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

  val DELETEMEgetProvideDetailsInProgress: ActionBuilder[IndividualProvideDetailsRequest, AnyContent] = DELETEMEgetProvidedDetails
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

  val getProvideDetailsWithApplicationInProgress: ActionBuilderWithData[DataWithAgentApplication] =
    getProvideDetailsInProgress
      .enrichWithAgentApplicationAction

  val DELETEMEgetProvideDetailsWithApplicationInProgress: ActionBuilder[
    IndividualProvideDetailsWithApplicationRequest,
    AnyContent
  ] = DELETEMEgetProvideDetailsInProgress.andThen(enrichWithAgentApplicationAction)

  val getSubmittedDetailsWithApplicationInProgress: ActionBuilderWithData[DataWithAgentApplication] = getProvidedDetails
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
    )
    .refineWithData(enricherAgentApplication.enrichRequest)

  val DELETEMEgetSubmitedDetailsWithApplicationInProgress: ActionBuilder[IndividualProvideDetailsWithApplicationRequest, AnyContent] =
    DELETEMEgetProvidedDetails
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

  extension [Data <: Tuple](ab: ActionBuilderWithData[Data])

    inline def enrichWithAgentApplicationAction(using
      AgentApplication AbsentIn Data,
      IndividualProvidedDetailsToBeDeleted PresentIn Data
    ): ActionBuilderWithData[AgentApplication *: Data] = ab
      .refineWithData:
        enricherAgentApplication.enrichRequest
