/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.testonly.controllers.applicant

import play.api.http.Status.SEE_OTHER
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Precreated
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantAuthRefiner
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfRequiredKeyIndividuals
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyData
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.CompletedSection.*
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.CompletedSection
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.withUpdatedIdentifiers
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.GrsStubService
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.StubUserService
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.FastForwardPage
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData
import uk.gov.hmrc.http.SessionKeys

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class FastForwardController @Inject() (
  mcc: MessagesControllerComponents,
  applicantActions: ApplicantActions,
  applicantAuthRefiner: ApplicantAuthRefiner,
  fastForwardPage: FastForwardPage,
  stubUserService: StubUserService,
  grsStubService: GrsStubService,
  applicationService: AgentApplicationService,
  agentApplicationIdGenerator: AgentApplicationIdGenerator,
  linkIdGenerator: LinkIdGenerator,
  individualProvideDetailsService: IndividualProvideDetailsService
)(using
  clock: Clock,
  ex: ExecutionContext
)
extends FrontendController(mcc, applicantActions):

  def show: Action[AnyContent] = applicantActions.action:
    implicit request =>
      Ok(fastForwardPage())

  def fastForward(
    completedSection: CompletedSection
  ): Action[AnyContent] =
    completedSection match
      case c: CompletedSectionLlp =>
        authorisedOrCreateAndLoginAgent.async: (req: RequestWithAuth) =>
          given RequestWithAuth = req
          fastForwardTo(c).map(_ => Redirect(AppRoutes.apply.TaskListController.show))
      case c: CompletedSectionGeneralPartnership =>
        authorisedOrCreateAndLoginAgent.async: (req: RequestWithAuth) =>
          given RequestWithAuth = req
          fastForwardTo(c).map(_ => Redirect(AppRoutes.apply.TaskListController.show))
      case c: CompletedSectionScottishPartnership =>
        authorisedOrCreateAndLoginAgent.async: (req: RequestWithAuth) =>
          given RequestWithAuth = req
          fastForwardTo(c).map(_ => Redirect(AppRoutes.apply.TaskListController.show))
      case c: CompletedSectionSoleTrader =>
        authorisedOrCreateAndLoginAgent.async: (req: RequestWithAuth) =>
          given RequestWithAuth = req
          fastForwardTo(c).map(_ => Redirect(AppRoutes.apply.TaskListController.show))
      case c: CompletedSectionLimitedCompany =>
        authorisedOrCreateAndLoginAgent.async: (req: RequestWithAuth) =>
          given RequestWithAuth = req
          fastForwardTo(c).map(_ => Redirect(AppRoutes.apply.TaskListController.show))
      case c: CompletedSectionLimitedPartnership =>
        authorisedOrCreateAndLoginAgent.async: (req: RequestWithAuth) =>
          given RequestWithAuth = req
          fastForwardTo(c).map(_ => Redirect(AppRoutes.apply.TaskListController.show))
      case c: CompletedSectionScottishLimitedPartnership =>
        authorisedOrCreateAndLoginAgent.async: (req: RequestWithAuth) =>
          given RequestWithAuth = req
          fastForwardTo(c).map(_ => Redirect(AppRoutes.apply.TaskListController.show))

  private def loginAndRetry(using request: Request[AnyContent]): Future[Result | RequestWithAuth] = stubUserService.createAndLoginAgent.map: stubsHc =>
    val bearerToken: String = stubsHc.authorization
      .map(_.value)
      .getOrThrowExpectedDataMissing("Expected auth token in stubs HeaderCarrier")

    val sessionId: String = stubsHc.sessionId
      .map(_.value)
      .getOrThrowExpectedDataMissing("Expected sessionId in stubs HeaderCarrier")

    Redirect(request.uri).addingToSession(
      SessionKeys.authToken -> bearerToken,
      SessionKeys.sessionId -> sessionId
    )

  private val authorisedOrCreateAndLoginAgent: ActionBuilderWithData[DataWithAuth] = applicantActions.action.refine:
    implicit request =>
      applicantAuthRefiner.refine(request).flatMap:
        case Right(authorisedRequest) => Future.successful(authorisedRequest)

        case Left(result) if result.header.status === SEE_OTHER => loginAndRetry

        case Left(result) => Future.successful(result)

  private def fastForwardTo(section: CompletedSection)(using r: RequestWithAuth): Future[Unit] =
    val toAppState: AgentApplication = section.agentApplication
    val maybeNumberOfKeyIndividuals: Option[NumberOfRequiredKeyIndividuals] = None

    for
      _ <- grsStubService.storeStubsData(
        businessType = section.businessType,
        journeyData = journeyDataFor(section.businessType, maybeNumberOfKeyIndividuals),
        deceased = false
      )
      updated <- updateIdentifiers(toAppState)
      _ <- applicationService.upsert(updated)
      _ <- upsertIndividuals(section, updated.agentApplicationId)
    yield ()

  private def upsertIndividuals(
    section: CompletedSection,
    applicationId: AgentApplicationId
  )(using r: RequestWithAuth): Future[Unit] =
    val howManyIndividuals: Int = 0
    val stubbedIndividuals = TestOnlyData.grsStubbedIndividuals
    if (howManyIndividuals > stubbedIndividuals.length)
      throw new RuntimeException(s"Only ${stubbedIndividuals.length} individuals are stubbed in grs currently")

    val individualProvidedDetailsState = section.maybeIndividualsList.map(_.providedDetailsState).getOrElse(Precreated)

    val createdIndividuals = stubbedIndividuals
      .take(howManyIndividuals)
      .map(_.copy(agentApplicationId = applicationId, providedDetailsState = individualProvidedDetailsState))

    createdIndividuals.foldLeft(Future.unit):
      (
        acc,
        individual
      ) =>
        for
          _ <- acc
          _ <- individualProvideDetailsService.upsertForApplication(individual)
          _ <- grsStubService.storeIndividualProvidedDetails(individual.individualName.value)
        yield ()

  private def journeyDataFor(
    bt: BusinessType,
    maybeNumOfTaxAdvisers: Option[NumberOfRequiredKeyIndividuals]
  ): JourneyData =
    (bt, maybeNumOfTaxAdvisers) match
      case (BusinessType.Partnership.LimitedLiabilityPartnership, Some(FiveOrLess(_))) => TestOnlyData.grs.llp.journeyDataTaxAdvisers2
      case (BusinessType.Partnership.LimitedLiabilityPartnership, Some(SixOrMore(_))) => TestOnlyData.grs.llp.journeyDataTaxAdvisers6
      case (BusinessType.Partnership.LimitedLiabilityPartnership, _) => TestOnlyData.grs.llp.journeyDataBase
      case (BusinessType.Partnership.GeneralPartnership, _) => TestOnlyData.grs.generalPartnership.journeyDataBase
      case (BusinessType.Partnership.ScottishPartnership, _) => TestOnlyData.grs.scottishPartnership.journeyDataBase
      case (BusinessType.Partnership.ScottishLimitedPartnership, _) => TestOnlyData.grs.scottishLtdPartnership.journeyDataBase
      case (BusinessType.Partnership.LimitedPartnership, _) => TestOnlyData.grs.ltdPartnership.journeyDataBase
      case (BusinessType.SoleTrader, _) => TestOnlyData.grs.soleTrader.journeyDataBase
      case (BusinessType.LimitedCompany, _) => TestOnlyData.grs.ltd.journeyDataBase

  private def updateIdentifiers(agentApplication: AgentApplication)(using
    r: RequestWithAuth,
    clock: Clock
  ): Future[AgentApplication] =
    val identifiers: Future[(AgentApplicationId, LinkId)] = applicationService.find().map:
      case Some(existingApplication) => (existingApplication.agentApplicationId, existingApplication.linkId)
      case None => (agentApplicationIdGenerator.nextApplicationId(), linkIdGenerator.nextLinkId())
    identifiers.map:
      case (id, linkId) =>
        agentApplication
          .withUpdatedIdentifiers(
            id,
            r.internalUserId,
            linkId,
            r.groupId,
            Instant.now(clock)
          )
