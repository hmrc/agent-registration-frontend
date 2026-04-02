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

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsIdGenerator
import uk.gov.hmrc.agentregistration.shared.lists.*
import uk.gov.hmrc.agentregistration.shared.risking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantAuthRefiner
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyData
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentRegistrationRiskingService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.CompletedSection.*
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.CompletedSection
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.PlanetId
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.UserId
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.withUpdatedIdentifiers
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.GrsStubService
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.StubUserService
import uk.gov.hmrc.agentregistrationfrontend.testonly.util.InternalUserIdGenerator
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.FastForwardPage
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdTestOnly

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.chaining.scalaUtilChainingOps
import StubUserService.addToSession
import play.api.mvc.request.Cell
import play.api.mvc.request.RequestAttrKey
import uk.gov.hmrc.agentregistrationfrontend.action.RequestWithDataCt
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.header.v2.HeaderNames

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
  individualProvideDetailsService: IndividualProvideDetailsService,
  agentRegistrationRiskingService: AgentRegistrationRiskingService,
  internalUserIdGenerator: InternalUserIdGenerator,
  individualProvidedDetailsIdGenerator: IndividualProvidedDetailsIdGenerator
)(using
  clock: Clock,
  ex: ExecutionContext
)
extends FrontendController(mcc, applicantActions):

  def show: Action[AnyContent] = applicantActions
    .action:
      implicit request =>
        Ok(fastForwardPage())

  def fastForward(completedSection: CompletedSection): Action[AnyContent] = applicantActions
    .action
    .async:
      implicit req: RequestWithData[EmptyData] =>
        fastForwardApplicantTo(completedSection)
          .map(agentApplicationId => Redirect(AppRoutes.testOnly.applicant.TestOnlyController.showAgentApplicationTile(agentApplicationId)))

  private def fastForwardApplicantTo(section: CompletedSection)(using request: RequestWithData[EmptyData]): Future[AgentApplicationId] =

    val agentApplicationId: AgentApplicationId = agentApplicationIdGenerator.nextApplicationId()
    val planetId: PlanetId = PlanetId.make(agentApplicationId)
    val userIdApplicant: UserId = UserId.make(agentApplicationId)

    for
      userApplicant <- stubUserService.createUserApplicant(userIdApplicant, planetId)
      loginResponse <- stubUserService.signIn(userApplicant)
      loggedInAsUserApplicantRequest: RequestWithDataCt[AnyContent, EmptyTuple] = RequestWithDataCt.empty(
        loginResponse.refineRequest(request.request)
      )
      loggedInAsUserApplicantRequestWithAuthData: RequestWithData[DataWithAuth] <- applicantAuthRefiner
        .refine(loggedInAsUserApplicantRequest)
        .map:
          case Right(data: RequestWithData[DataWithAuth]) => data
          case Left(r) => throw new RuntimeException(s"ApplicantAuthRefiner didn't fetch DataWithAuth: $r")

      agentApplication = section.agentApplication.withUpdatedIdentifiers(
        id = agentApplicationId,
        internalUserId = loggedInAsUserApplicantRequestWithAuthData.get[InternalUserId],
        linkId = linkIdGenerator.nextLinkId(),
        groupId = loggedInAsUserApplicantRequestWithAuthData.get[GroupId],
        createdAt = Instant.now(clock)
      )
      _ <- applicationService.upsert(agentApplication)(using loggedInAsUserApplicantRequest)
      _ <-
        grsStubService.storeStubsData(
          businessType = section.businessType,
          journeyData = journeyDataFor(section.businessType),
          deceased = false
        )(using loggedInAsUserApplicantRequest)

      individuals <- section
        .maybeIndividualProvidedDetailsList
        .getOrElse(Nil)
        .zipWithIndex
        .map: (t: (IndividualProvidedDetails, Int)) =>
          updateIndividualProvidedDetails(
            individualProvidedDetails = t._1,
            agentApplicationId = agentApplication.agentApplicationId,
            individualName = getIndividualName(t._2)
          )
        .map(i => individualProvideDetailsService.upsertForApplication(i).map(_ => i))
        .pipe(Future.sequence)
      //      _ <- individuals
      //        .map: individual =>
      //          val saUtr = TdTestOnly.saUtr.asUtr // TODO: this has to come from completed section
      //          companiesHouseIndividualService.storeIndividualProvidedDetails(individual.individualName.value, Some(saUtr))
      //        .pipe(Future.sequence)
      _ <- sendForRiskingIfNeeded(agentApplication, individuals)
    yield agentApplicationId

  private def sendForRiskingIfNeeded(
    agentApplication: AgentApplication,
    individuals: List[IndividualProvidedDetails]
  )(using request: RequestHeader) =
    if agentApplication.applicationState.sentForRisking
    then
      agentRegistrationRiskingService.submitForRisking(
        submitForRiskingRequest = SubmitForRiskingRequest(
          agentApplication = agentApplication,
          individuals = individuals
        )
      )
    else Future.unit

  private def removeStubbedIndividualsFor(applicationId: AgentApplicationId)(using r: RequestWithAuth): Future[Unit] =
    for
      individuals: List[IndividualProvidedDetails] <- individualProvideDetailsService.findAllByApplicationId(applicationId)
      _ <- Future.sequence(
        individuals.map: ipd =>
          individualProvideDetailsService.delete(ipd.individualProvidedDetailsId)
      )
    yield ()

  //  private def createIndividualProvidedDetailsList(
  //    individualProvidedDetailsList: List[IndividualProvidedDetails],
  //    agentApplicationId: AgentApplicationId
  //  )(using
  //    clock: Clock
  //  ): Future[List[IndividualProvidedDetails]] =
  //    Future.traverse(individualProvidedDetailsList.zipWithIndex):
  //      case (tdIndividualProvidedDetails, index) =>
  //        val stubbedName = getIndividualName(index)
  //        Future.successful(tdIndividualProvidedDetails.copy(
  //          _id = individualProvidedDetailsIdGenerator.nextIndividualProvidedDetailsId(),
  //          individualName = stubbedName,
  //          agentApplicationId = agentApplicationId,
  //          internalUserId = tdIndividualProvidedDetails.internalUserId.map(_ => internalUserIdGenerator.nextInternalUserId()),
  //          createdAt = Instant.now(clock)
  //        ))

  private def updateIndividualProvidedDetails(
    individualProvidedDetails: IndividualProvidedDetails,
    agentApplicationId: AgentApplicationId,
    individualName: IndividualName
  ): IndividualProvidedDetails = individualProvidedDetails.copy(
    _id = individualProvidedDetailsIdGenerator.nextIndividualProvidedDetailsId(),
    individualName = individualName,
    agentApplicationId = agentApplicationId,
    internalUserId = individualProvidedDetails.internalUserId.map(_ => internalUserIdGenerator.nextInternalUserId()),
    createdAt = Instant.now(clock)
  )

  private def getIndividualName(index: Int): IndividualName = TdTestOnly // TODO: this has to compre from completedSection
    .individualNamesStubbedInCompaniesHouse
    .lift(index)
    .getOrThrowExpectedDataMissing(s"No identity stubbed at index $index")

  //  private def upsertIndividuals(
  //    createdIndividuals: List[IndividualProvidedDetails]
  //  )(using r: RequestWithAuth): Future[Unit] =
  //    createdIndividuals.foldLeft(Future.unit):
  //      (
  //        acc,
  //        individual
  //      ) =>
  //        for
  //          _ <- acc
  //          _ <- individualProvideDetailsService.upsertForApplication(individual)
  //          _ <- companiesHouseIndividualService.storeIndividualProvidedDetails(individual.individualName.value, Some(TestOnlyData.saUtr.asUtr))
  //        yield ()

  private def journeyDataFor(
    bt: BusinessType
  ): JourneyData =
    bt match
      case BusinessType.Partnership.LimitedLiabilityPartnership => TdTestOnly.grsJourneyData.llp.journeyData
      case BusinessType.Partnership.GeneralPartnership => TdTestOnly.grsJourneyData.generalPartnership.journeyData
      case BusinessType.Partnership.ScottishPartnership => TdTestOnly.grsJourneyData.scottishPartnership.journeyData
      case BusinessType.Partnership.ScottishLimitedPartnership => TdTestOnly.grsJourneyData.scottishLtdPartnership.journeyData
      case BusinessType.Partnership.LimitedPartnership => TdTestOnly.grsJourneyData.ltdPartnership.journeyData
      case BusinessType.SoleTrader => TdTestOnly.grsJourneyData.soleTrader.journeyData
      case BusinessType.LimitedCompany => TdTestOnly.grsJourneyData.ltd.journeyData
