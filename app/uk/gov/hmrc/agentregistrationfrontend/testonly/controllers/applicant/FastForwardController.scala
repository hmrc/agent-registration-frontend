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
import uk.gov.hmrc.agentregistration.shared.util.PathBindableFactory
import uk.gov.hmrc.agentregistration.shared.util.SealedObjects
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.action.ApplicantActions.DataWithAuth
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.AuthorisedActionRefiner
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.testonly.controllers.applicant.FastForwardController.CompletedSection
import uk.gov.hmrc.agentregistrationfrontend.testonly.controllers.applicant.FastForwardController.CompletedSection.CompletedSectionLlp
import uk.gov.hmrc.agentregistrationfrontend.testonly.controllers.applicant.FastForwardController.CompletedSection.CompletedSectionSoleTrader
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.GrsStubService
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.StubUserService
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.FastForwardPage
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

object FastForwardController:

  sealed trait CompletedSection:

    def sectionName: String
    def businessType: BusinessType
    def alreadyDeveloped: Boolean = false
    def displayOrder: Int

  object CompletedSection:

    sealed trait CompletedSectionLlp
    extends CompletedSection:
      override final def businessType: BusinessType = BusinessType.Partnership.LimitedLiabilityPartnership

    object CompletedSectionLlp:

      case object LlpAboutYourBusiness
      extends CompletedSectionLlp:

        override def sectionName: String = "About your business"
        override def alreadyDeveloped: Boolean = true
        override def displayOrder: Int = 1

      case object LlpApplicantContactDetails
      extends CompletedSectionLlp:

        override def sectionName: String = "Applicant Contact Details"
        override def alreadyDeveloped: Boolean = true
        override def displayOrder: Int = 2

      case object LlpAgentServicesAccountDetails
      extends CompletedSectionLlp:

        override def sectionName: String = "Agent services account details"
        override def alreadyDeveloped: Boolean = true
        override def displayOrder: Int = 3

      case object LlpAntiMoneyLaunderingSupervisionDetails
      extends CompletedSectionLlp:

        override def sectionName: String = "Anti-money laundering supervision details"
        override def alreadyDeveloped: Boolean = true
        override def displayOrder: Int = 4

      case object LlpHmrcStandardForAgents
      extends CompletedSectionLlp:

        override def sectionName: String = "HMRC standard for agents"
        override def alreadyDeveloped: Boolean = true
        override def displayOrder: Int = 5

      case object LlpDeclaration
      extends CompletedSectionLlp:

        override def sectionName: String = "Declaration"
        override def alreadyDeveloped: Boolean = true
        override def displayOrder: Int = 6

      val values: Seq[CompletedSectionLlp] = SealedObjects.all[CompletedSectionLlp]

    sealed trait CompletedSectionSoleTrader
    extends CompletedSection:
      override def businessType: BusinessType = BusinessType.SoleTrader

    object CompletedSectionSoleTrader:
      // example:
      case object SoleTraderAboutYourBusiness
      extends CompletedSectionSoleTrader:

        override def sectionName: String = "About your business"
        override def alreadyDeveloped: Boolean = true
        override def displayOrder: Int = 1

    val values: Seq[CompletedSection] = SealedObjects.all[CompletedSection]
    given PathBindable[CompletedSection] = PathBindableFactory.makeSealedObjectPathBindable[CompletedSection]

@Singleton
class FastForwardController @Inject() (
  mcc: MessagesControllerComponents,
  applicantActions: ApplicantActions,
  authorisedActionRefiner: AuthorisedActionRefiner,
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

  private def loginAndRetry(using request: Request[AnyContent]): Future[Either[Result, RequestWithAuth]] = stubUserService.createAndLoginAgent.map: stubsHc =>
    val bearerToken: String = stubsHc.authorization
      .map(_.value)
      .getOrThrowExpectedDataMissing("Expected auth token in stubs HeaderCarrier")

    val sessionId: String = stubsHc.sessionId
      .map(_.value)
      .getOrThrowExpectedDataMissing("Expected sessionId in stubs HeaderCarrier")

    Left(
      Redirect(request.uri).addingToSession(
        SessionKeys.authToken -> bearerToken,
        SessionKeys.sessionId -> sessionId
      )
    )

  private val authorisedOrCreateAndLoginAgent: ActionBuilderWithData[DataWithAuth] = applicantActions.action.refineAsync:
    implicit request =>
      authorisedActionRefiner.refine(request).flatMap:
        case Right(authorisedRequest) => Future.successful(Right(authorisedRequest))
        case Left(result) if result.header.status === SEE_OTHER => loginAndRetry
        case Left(result) => Future.successful(Left(result))

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

  private def updateIdentifiers(agentApplication: AgentApplicationLlp)(using
    r: RequestWithAuth,
    clock: Clock
  ): Future[AgentApplicationLlp] =
    val identifiers: Future[(AgentApplicationId, LinkId)] = applicationService.find().map:
      case Some(existingApplication) => (existingApplication.agentApplicationId, existingApplication.linkId)
      case None => (agentApplicationIdGenerator.nextApplicationId(), linkIdGenerator.nextLinkId())
    identifiers.map: t =>
      agentApplication.copy(
        _id = t._1,
        internalUserId = r.internalUserId,
        linkId = t._2,
        groupId = r.groupId,
        createdAt = Instant.now(clock)
      )
