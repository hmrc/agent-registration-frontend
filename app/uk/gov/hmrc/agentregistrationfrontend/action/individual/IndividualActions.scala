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
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuilders.refineFutureEither
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuilders.refineUnion
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuildersWithData
import uk.gov.hmrc.agentregistrationfrontend.action.RequestWithDataCt
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

  type DataWithAgentApplicationFromLinkId = AgentApplication *: DataWithAdditionalIdentifiers
  type RequestWithAgentApplication = RequestWithData[DataWithAgentApplicationFromLinkId]
  type RequestWithAgentApplicationCt[ContentType] = RequestWithDataCt[ContentType, DataWithAgentApplicationFromLinkId]

  type DataWithIndividualProvidedDetails = IndividualProvidedDetails *: DataWithAgentApplicationFromLinkId
  type RequestWithIndividualProvidedDetails = RequestWithData[DataWithIndividualProvidedDetails]
  type RequestWithIndividualProvidedDetailsCt[ContentType] = RequestWithDataCt[ContentType, DataWithIndividualProvidedDetails]

  // the request and data types below are renamed with "ToBeDeleted" are predicated on the deprecated model and pattern
  // of generating new IndividualProvidedDetails records for users without one
  type DataWithIndividualProvidedDetailsToBeDeleted = IndividualProvidedDetailsToBeDeleted *: DataWithAuth
  type RequestWithIndividualProvidedDetailsToBeDeleted = RequestWithData[DataWithIndividualProvidedDetailsToBeDeleted]
  type RequestWithIndividualProvidedDetailsToBeDeletedCt[ContentType] = RequestWithDataCt[ContentType, DataWithIndividualProvidedDetailsToBeDeleted]

  type DataWithAgentApplicationToBeDeleted = AgentApplication *: DataWithIndividualProvidedDetailsToBeDeleted
  type RequestWithAgentApplicationToBeDeleted = RequestWithData[DataWithAgentApplicationToBeDeleted]
  type RequestWithAgentApplicationToBeDeletedCt[ContentType] = RequestWithDataCt[ContentType, DataWithAgentApplicationToBeDeleted]

@Singleton
class IndividualActions @Inject() (
  defaultActionBuilder: DefaultActionBuilder,
  individualAuthorisedRefiner: IndividualAuthRefiner,
  individualProvideDetailsRefiner: IndividualProvideDetailsRefiner,
  enricherAgentApplication: AgentApplicationEnricher
)(using ExecutionContext)
extends RequestAwareLogging:

  export ActionBuildersWithData.*
  export IndividualActions.*

  val action: ActionBuilderWithData[EmptyTuple] = defaultActionBuilder
    .refineUnion(request => RequestWithDataCt.empty(request))

  val authorised: ActionBuilderWithData[DataWithAuth] = action
    .refineFutureEither(individualAuthorisedRefiner.refineIntoRequestWithAuth)

  val authorisedWithAdditionalIdentifiers: ActionBuilderWithData[DataWithAdditionalIdentifiers] = action
    .refineFutureEither(individualAuthorisedRefiner.refineIntoRequestWithAdditionalIdentifiers)

  val getProvidedDetailsToBeDeleted: ActionBuilderWithData[DataWithIndividualProvidedDetailsToBeDeleted] = authorised
    .refineFutureEither:
      individualProvideDetailsRefiner.refineIntoRequestWithIndividualProvidedDetailsToBeDeleted

  val getProvideDetailsInProgress: ActionBuilderWithData[DataWithIndividualProvidedDetailsToBeDeleted] = getProvidedDetailsToBeDeleted
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

  val getProvideDetailsWithApplicationInProgress: ActionBuilderWithData[DataWithAgentApplicationToBeDeleted] =
    getProvideDetailsInProgress
      .enrichWithAgentApplicationAction

  val getSubmittedDetailsWithApplicationInProgress: ActionBuilderWithData[DataWithAgentApplicationToBeDeleted] = getProvidedDetailsToBeDeleted
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
    .refine(enricherAgentApplication.enrichRequest)

  extension [Data <: Tuple](ab: ActionBuilderWithData[Data])

    inline def enrichWithAgentApplicationAction(using
      AgentApplication AbsentIn Data,
      IndividualProvidedDetailsToBeDeleted PresentIn Data
    ): ActionBuilderWithData[AgentApplication *: Data] = ab
      .refine:
        enricherAgentApplication.enrichRequest
