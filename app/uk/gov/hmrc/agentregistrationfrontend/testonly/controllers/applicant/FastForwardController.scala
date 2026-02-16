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

import org.apache.pekko.Done
import play.api.http.Status.SEE_OTHER
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantAuthRefiner
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyData
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.CompletedSection.*
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.CompletedSection
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.withUpdatedIdentifiers
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.GrsStubService
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.StubUserService
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.FastForwardPage
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
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
  simplePage: SimplePage,
  stubUserService: StubUserService,
  grsStubService: GrsStubService,
  applicationService: AgentApplicationService,
  agentApplicationIdGenerator: AgentApplicationIdGenerator,
  linkIdGenerator: LinkIdGenerator
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
      case other =>
        applicantActions.action { implicit request =>
          Ok(
            simplePage(
              h1 = s"${other.businessType} Task List Page",
              bodyText = Some(s"Fast Forwarding to ${other.businessType} List Page isn't implemented yet")
            )
          )
        }

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

  private def fastForwardTo(section: CompletedSection)(using r: RequestWithAuth): Future[Done] =
    val toAppState: AgentApplication = section.appState

    for {
      _ <- grsStubService.storeStubsData(
        businessType = section.businessType,
        journeyData = journeyDataFor(section.businessType),
        deceased = false
      )
      updated <- updateIdentifiers(toAppState)
      _ <- applicationService.upsert(updated)
    } yield Done

  private def journeyDataFor(bt: BusinessType): JourneyData =
    bt match
      case BusinessType.Partnership.LimitedLiabilityPartnership => TestOnlyData.grs.llp.journeyData
      case BusinessType.Partnership.GeneralPartnership => TestOnlyData.grs.generalPartnership.journeyData
      case BusinessType.Partnership.ScottishPartnership => TestOnlyData.grs.scottishPartnership.journeyData
      case _ => throw new IllegalArgumentException(s"Add $bt journey data here")

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
