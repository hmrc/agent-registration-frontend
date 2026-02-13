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
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions.DataWithAuth
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantAuthRefiner
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.CompletedSection
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.CompletedSection.*
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.GrsStubService
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.StubUserService
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.FastForwardPage
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.withUpdatedIdentifiers

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class FastForwardController @Inject() (
  mcc: MessagesControllerComponents,
  applicantActions: ApplicantActions,
  applicantAuthRefiner: ApplicantAuthRefiner,
  applicationService: AgentApplicationService,
  fastForwardPage: FastForwardPage,
  agentApplicationIdGenerator: AgentApplicationIdGenerator,
  linkIdGenerator: LinkIdGenerator,
  simplePage: SimplePage,
  grsStubService: GrsStubService,
  stubUserService: StubUserService
)(using clock: Clock)
extends FrontendController(mcc, applicantActions):

  def show: Action[AnyContent] = applicantActions.action:
    implicit request =>
      Ok(fastForwardPage())

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

  def fastForward(
    completedSection: CompletedSection
  ): Action[AnyContent] =
    completedSection match
      case c: CompletedSectionLlp =>
        authorisedOrCreateAndLoginAgent.async: (req: RequestWithAuth) =>
          given RequestWithAuth = req
          handleCompletedSectionLlp(c)
      case _: CompletedSectionSoleTrader =>
        applicantActions.action { implicit request =>
          Ok(
            simplePage(
              h1 = "Sole Trader Task List Page",
              bodyText = Some("Fast Forwarding to Sole Trader Task List Page isn't implemented yet")
            )
          )
        }
      case c: CompletedSectionGeneralPartnership =>
        authorisedOrCreateAndLoginAgent.async: (req: RequestWithAuth) =>
          given RequestWithAuth = req
          handleCompletedSectionGeneralPartnership(c)
      case c: CompletedSectionScottishPartnership =>
        authorisedOrCreateAndLoginAgent.async: (req: RequestWithAuth) =>
          given RequestWithAuth = req
          handleCompletedSectionScottishPartnership(c)

  private def handleCompletedSectionLlp(completedSection: CompletedSectionLlp)(using
    r: RequestWithAuth,
    clock: Clock
  ): Future[Result] =
    val application =
      (completedSection match
        case CompletedSectionLlp.LlpAboutYourBusiness => TestOnlyData.agentApplicationLlp.afterCompaniesHouseStatusCheckPass
        case CompletedSectionLlp.LlpApplicantContactDetails => TestOnlyData.agentApplicationLlp.afterContactDetailsComplete
        case CompletedSectionLlp.LlpAgentServicesAccountDetails => TestOnlyData.agentApplicationLlp.afterAgentDetailsComplete
        case CompletedSectionLlp.LlpAntiMoneyLaunderingSupervisionDetails => TestOnlyData.agentApplicationLlp.afterAmlsComplete
        case CompletedSectionLlp.LlpHmrcStandardForAgents => TestOnlyData.agentApplicationLlp.afterHmrcStandardForAgentsAgreed
        case CompletedSectionLlp.LlpDeclaration => TestOnlyData.agentApplicationLlp.afterDeclarationSubmitted
      )

    for {
      _ <- grsStubService.storeStubsData(
        businessType = application.businessType,
        journeyData = TestOnlyData.grs.llp.journeyData,
        deceased = false
      )
      updatedApp <- updateIdentifiers(application)
      _ <- applicationService.upsert(updatedApp)
    } yield Redirect(AppRoutes.apply.TaskListController.show)

  private def handleCompletedSectionGeneralPartnership(completedSection: CompletedSectionGeneralPartnership)(using
    r: RequestWithAuth,
    clock: Clock
  ): Future[Result] =
    val application =
      completedSection match
        case CompletedSectionGeneralPartnership.GeneralPartnershipAboutYourBusiness =>
          TestOnlyData.agentApplicationGeneralPartnership.afterRefusalToDealWithCheckPass
        case CompletedSectionGeneralPartnership.GeneralPartnershipApplicantContactDetails =>
          TestOnlyData.agentApplicationGeneralPartnership.afterContactDetailsComplete
        case CompletedSectionGeneralPartnership.GeneralPartnershipAgentServicesAccountDetails =>
          TestOnlyData.agentApplicationGeneralPartnership.afterAgentDetailsComplete
        case CompletedSectionGeneralPartnership.GeneralPartnershipAntiMoneyLaunderingSupervisionDetails =>
          TestOnlyData.agentApplicationGeneralPartnership.afterAmlsComplete
        case CompletedSectionGeneralPartnership.GeneralPartnershipHmrcStandardForAgents =>
          TestOnlyData.agentApplicationGeneralPartnership.afterHmrcStandardForAgentsAgreed
        case CompletedSectionGeneralPartnership.GeneralPartnershipDeclaration => TestOnlyData.agentApplicationGeneralPartnership.afterDeclarationSubmitted

    for {
      _ <- grsStubService.storeStubsData(
        businessType = application.businessType,
        journeyData = TestOnlyData.grs.generalPartnership.journeyData,
        deceased = false
      )
      updatedApp <- updateIdentifiers(application)
      _ <- applicationService.upsert(updatedApp)
    } yield Redirect(AppRoutes.apply.TaskListController.show)

  private def handleCompletedSectionScottishPartnership(completedSection: CompletedSectionScottishPartnership)(using
    r: RequestWithAuth,
    clock: Clock
  ): Future[Result] =
    val application =
      (completedSection match
        case CompletedSectionScottishPartnership.ScottishPartnershipAboutYourBusiness =>
          TestOnlyData.agentApplicationScottishPartnership.afterRefusalToDealWithCheckPass
        case CompletedSectionScottishPartnership.ScottishPartnershipApplicantContactDetails =>
          TestOnlyData.agentApplicationScottishPartnership.afterContactDetailsComplete
        case CompletedSectionScottishPartnership.ScottishPartnershipAgentServicesAccountDetails =>
          TestOnlyData.agentApplicationScottishPartnership.afterAgentDetailsComplete
        case CompletedSectionScottishPartnership.ScottishPartnershipAntiMoneyLaunderingSupervisionDetails =>
          TestOnlyData.agentApplicationScottishPartnership.afterAmlsComplete
        case CompletedSectionScottishPartnership.ScottishPartnershipHmrcStandardForAgents =>
          TestOnlyData.agentApplicationScottishPartnership.afterHmrcStandardForAgentsAgreed
        case CompletedSectionScottishPartnership.ScottishPartnershipDeclaration => TestOnlyData.agentApplicationScottishPartnership.afterDeclarationSubmitted
      )

    for {
      _ <- grsStubService.storeStubsData(
        businessType = application.businessType,
        journeyData = TestOnlyData.grs.scottishPartnership.journeyData,
        deceased = false
      )
      updatedApp <- updateIdentifiers(application)
      _ <- applicationService.upsert(updatedApp)
    } yield Redirect(AppRoutes.apply.TaskListController.show)

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
