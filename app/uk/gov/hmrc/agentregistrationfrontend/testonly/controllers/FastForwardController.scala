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

package uk.gov.hmrc.agentregistrationfrontend.testonly.controllers

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.util.PathBindableFactory
import uk.gov.hmrc.agentregistration.shared.util.SealedObjects
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.testonly.controllers.FastForwardController.CompletedSection
import uk.gov.hmrc.agentregistrationfrontend.testonly.controllers.FastForwardController.CompletedSection.CompletedSectionLlp
import uk.gov.hmrc.agentregistrationfrontend.testonly.controllers.FastForwardController.CompletedSection.CompletedSectionSoleTrader
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.GrsStubService
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.FastForwardPage
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
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
  actions: Actions,
  applicationService: AgentApplicationService,
  fastForwardPage: FastForwardPage,
  agentApplicationIdGenerator: AgentApplicationIdGenerator,
  linkIdGenerator: LinkIdGenerator,
  simplePage: SimplePage,
  grsStubService: GrsStubService
)(using clock: Clock)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] = actions.action { implicit request =>
    Ok(fastForwardPage())
  }

  def fastForward(
    completedSection: CompletedSection
  ): Action[AnyContent] =
    completedSection match
      case c: CompletedSectionLlp => actions.Applicant.authorised.async(handleCompletedSectionLlp(c)(using _, summon))
      case c: CompletedSectionSoleTrader =>
        actions.action { implicit request =>
          Ok(simplePage(
            h1 = "Sole Trader Task List Page",
            bodyText = Some("Fast Forwarding to Sole Trader Task List Page isn't implemented yet")
          ))
        }
      // TODO: other business types

  private def handleCompletedSectionLlp(completedSection: CompletedSectionLlp)(using
    r: AuthorisedRequest[AnyContent],
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
      _ <- applicationService.deleteMeUpsert(updatedApp)
    } yield Redirect(AppRoutes.apply.TaskListController.show)

  private def updateIdentifiers(agentApplication: AgentApplicationLlp)(using
    r: AuthorisedRequest[AnyContent],
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
