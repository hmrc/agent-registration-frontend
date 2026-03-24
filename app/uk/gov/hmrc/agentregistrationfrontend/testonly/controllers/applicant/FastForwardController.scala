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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsIdGenerator
import uk.gov.hmrc.agentregistration.shared.lists.*
import uk.gov.hmrc.agentregistration.shared.risking.SubmitForRiskingRequest
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantAuthRefiner
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyData
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentRegistrationRiskingService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.CompletedSection.*
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.CompletedSection
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.withUpdatedIdentifiers
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.CompaniesHouseIndividualService
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.GrsStubService
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.StubUserService
import uk.gov.hmrc.agentregistrationfrontend.testonly.util.InternalUserIdGenerator
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.FastForwardPage
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData
import uk.gov.hmrc.http.SessionKeys

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.chaining.scalaUtilChainingOps

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
                                        individualProvidedDetailsIdGenerator: IndividualProvidedDetailsIdGenerator,
                                        companiesHouseIndividualService: CompaniesHouseIndividualService
                                      )(using
                                        clock: Clock,
                                        ex: ExecutionContext
                                      )
  extends FrontendController(mcc, applicantActions):

  def show: Action[AnyContent] = applicantActions.action:
    implicit request =>
      Ok(fastForwardPage())

  def fastForward(completedSection: CompletedSection): Action[AnyContent] = authorisedOrCreateAndLoginAgent
    .async:
      implicit req: RequestWithAuth =>
        fastForwardTo(completedSection)
          .map(_ => Redirect(AppRoutes.apply.TaskListController.show))

  // TODO: always new user so no need to sync with DB and with Risking
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
    for
      _ <- grsStubService.storeStubsData(
        businessType = section.businessType,
        journeyData = journeyDataFor(section.businessType),
        deceased = false
      )
      agentApplication <- updateAgentApplication(section.agentApplication)
      _ <- applicationService.upsert(agentApplication)
      removeIndividuals <- removeStubbedIndividualsFor(agentApplication.agentApplicationId)
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
      _ <- individuals
        .map: individual =>
          companiesHouseIndividualService.storeIndividualProvidedDetails(individual.individualName.value, Some(TestOnlyData.saUtr.asUtr))
        .pipe(Future.sequence)
      _ <- sendForRiskingIfNeeded(agentApplication, individuals)
    yield ()

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
                                             )(using
                                               clock: Clock
                                             ): IndividualProvidedDetails = individualProvidedDetails.copy(
    _id = individualProvidedDetailsIdGenerator.nextIndividualProvidedDetailsId(),
    individualName = individualName,
    agentApplicationId = agentApplicationId,
    internalUserId = individualProvidedDetails.internalUserId.map(_ => internalUserIdGenerator.nextInternalUserId()),
    createdAt = Instant.now(clock)
  )

  private def getIndividualName(index: Int): IndividualName = TestOnlyData
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
      case BusinessType.Partnership.LimitedLiabilityPartnership => GrsTestData.grs.llp.journeyData
      case BusinessType.Partnership.GeneralPartnership => GrsTestData.grs.generalPartnership.journeyData
      case BusinessType.Partnership.ScottishPartnership => GrsTestData.grs.scottishPartnership.journeyData
      case BusinessType.Partnership.ScottishLimitedPartnership => GrsTestData.grs.scottishLtdPartnership.journeyData
      case BusinessType.Partnership.LimitedPartnership => GrsTestData.grs.ltdPartnership.journeyData
      case BusinessType.SoleTrader => GrsTestData.grs.soleTrader.journeyData
      case BusinessType.LimitedCompany => GrsTestData.grs.ltd.journeyData

  private def updateAgentApplication(agentApplication: AgentApplication)(using
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
            id = id,
            internalUserId = r.internalUserId,
            linkId = linkId,
            groupId = r.groupId,
            createdAt = Instant.now(clock)
          )
