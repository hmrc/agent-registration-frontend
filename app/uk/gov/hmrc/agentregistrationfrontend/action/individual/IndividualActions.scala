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
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationRiskingResponse
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuilders.refineFutureEither
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuilders.refineUnion
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuildersWithData
import uk.gov.hmrc.agentregistrationfrontend.action.RequestWithDataCt
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentRegistrationRiskingService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.Credentials

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

object IndividualActions:

  export uk.gov.hmrc.agentregistrationfrontend.action.Actions.*

  type DataWithAuth = (InternalUserId, Credentials)
  type RequestWithAuth = RequestWithData[DataWithAuth]
  type RequestWithAuthCt[ContentType] = RequestWithDataCt[ContentType, DataWithAuth]

  type DataWithAuthAndCl = ConfidenceLevel *: DataWithAuth
  type RequestWithAuthAndCl = RequestWithData[DataWithAuthAndCl]
  type RequestWithAuthAndClCt[ContentType] = RequestWithDataCt[ContentType, DataWithAuthAndCl]

  type DataWithAdditionalIdentifiers = Option[Nino] *: Option[SaUtr] *: DataWithAuthAndCl
  type RequestWithAdditionalIdentifiers = RequestWithData[DataWithAdditionalIdentifiers]
  type RequestWithAdditionalIdentifiersCt[ContentType] = RequestWithDataCt[ContentType, DataWithAdditionalIdentifiers]

  private type DataWithApplicationFromLinkId = AgentApplication *: DataWithAuthAndCl

  type DataWithIndividualProvidedDetails = IndividualProvidedDetails *: DataWithApplicationFromLinkId
  type RequestWithIndividualProvidedDetails = RequestWithData[DataWithIndividualProvidedDetails]
  type DataWithIndividualProvidedDetailsCt[ContentType] = RequestWithDataCt[ContentType, DataWithIndividualProvidedDetails]

  type DataWithRiskingProgress = ApplicationRiskingResponse *: DataWithIndividualProvidedDetails

@Singleton
class IndividualActions @Inject() (
  defaultActionBuilder: DefaultActionBuilder,
  individualAuthorisedRefiner: IndividualAuthRefiner,
  agentApplicationService: AgentApplicationService,
  individualProvideDetailsService: IndividualProvideDetailsService,
  agentRegistrationRiskingService: AgentRegistrationRiskingService
)(using ExecutionContext)
extends RequestAwareLogging:

  export ActionBuildersWithData.*
  export IndividualActions.*

  val action: ActionBuilderWithData[EmptyTuple] = defaultActionBuilder
    .refineUnion(request => RequestWithDataCt.empty(request))

  val authorised: ActionBuilderWithData[DataWithAuthAndCl] = action
    .refineFutureEither(individualAuthorisedRefiner.refineIntoRequestWithAuth)

  val authorisedWithAdditionalIdentifiers: ActionBuilderWithData[DataWithAdditionalIdentifiers] = action
    .refineFutureEither(individualAuthorisedRefiner.refineIntoRequestWithAdditionalIdentifiers)

  def authorisedWithIndividualProvidedDetails(linkId: LinkId): ActionBuilderWithData[DataWithIndividualProvidedDetails] = authorised
    .refine(implicit request =>
      agentApplicationService
        .find(linkId)
        .map:
          case Some(agentApplication) if agentApplication.hasFinished =>
            Redirect(AppRoutes.providedetails.riskingprogress.RiskingProgressController.show(linkId))
          case Some(agentApplication) => request.add[AgentApplication](agentApplication)
          case None => Redirect(AppRoutes.providedetails.ExitController.genericExitPage.url)
    )
    .refine(implicit request =>
      individualProvideDetailsService
        .findAllForMatchingWithApplication(request.get[AgentApplication].agentApplicationId)
        .map[RequestWithData[DataWithIndividualProvidedDetails] | Result]:
          case list: List[IndividualProvidedDetails] =>
            list
              .find(_.internalUserId.contains(request.get[InternalUserId]))
              .map(request.add[IndividualProvidedDetails])
              .getOrElse(
                Redirect(AppRoutes.providedetails.MatchIndividualProvidedDetailsController.show(linkId, fromIv = None))
              )
    )

  def authorisedWithRiskingProgress(linkId: LinkId): ActionBuilderWithData[DataWithRiskingProgress] = authorised
    .refine(implicit request =>
      agentApplicationService
        .find(linkId)
        .map:
          case Some(agentApplication) if agentApplication.hasFinished => request.add[AgentApplication](agentApplication)
          case Some(agentApplication) => Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId))
          case None => Redirect(AppRoutes.providedetails.ExitController.genericExitPage.url)
    )
    .refine(implicit request =>
      individualProvideDetailsService
        .findAllForMatchingWithApplication(request.get[AgentApplication].agentApplicationId)
        .map[RequestWithData[DataWithIndividualProvidedDetails] | Result]:
          case list: List[IndividualProvidedDetails] =>
            list
              .find(_.internalUserId.contains(request.get[InternalUserId]))
              .map(request.add[IndividualProvidedDetails])
              .getOrElse(
                Redirect(AppRoutes.providedetails.MatchIndividualProvidedDetailsController.show(linkId, fromIv = None))
              )
    )
    .refine(implicit request =>
      val personReference = request.get[IndividualProvidedDetails].personReference
      // TODO: this is an agent endpoint not for individuals, we will be getting a new endpoint in parallel branch
      agentRegistrationRiskingService
        .getApplicationRiskingResponse(request.get[AgentApplication].applicationReference)
        .map:
          case Some(riskingProgress) => request.add[ApplicationRiskingResponse](riskingProgress)
          case _ =>
            logger.warn(s"No risking progress found for the application, redirecting to check your answers - linkId: $linkId, personReference: $personReference")
            Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url)
    )
