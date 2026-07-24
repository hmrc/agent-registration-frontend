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

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessTypeAnswer
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.TestOnlyLink
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.TestApplicationService
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.TestRiskingService
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.ShowRecentApplicationsPage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.ShowAgentApplicationsTilePage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.TestLinkPage
import uk.gov.hmrc.agentregistrationfrontend.connectors.IndividualProvidedDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.testonly.action.TestOnlyActions
import uk.gov.hmrc.agentregistrationfrontend.testonly.connectors.TestAgentRegistrationConnector

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class TestOnlyController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  testApplicationService: TestApplicationService,
  testAgentRegistrationConnector: TestAgentRegistrationConnector,
  testOnlyActions: TestOnlyActions,
  testLinkPage: TestLinkPage,
  showRecentApplicationsPage: ShowRecentApplicationsPage,
  showAgentApplicationsTilePage: ShowAgentApplicationsTilePage,
  individualProvidedDetailsConnector: IndividualProvidedDetailsConnector,
  testRiskingService: TestRiskingService
)
extends FrontendController(mcc, actions):

  def showAgentApplication: Action[AnyContent] = actions
    .getApplication: request =>
      Ok(Json.prettyPrint(Json.toJson(request.agentApplication)))

  def showRecentAgentApplications(
    page: Int,
    pageSize: Int
  ): Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        for
          applications <- testAgentRegistrationConnector.getRecentApplications(page, pageSize)
          applicationsWithIndividuals: Seq[(AgentApplication, List[IndividualProvidedDetails])] <- Future.sequence:
            applications.map: agentApplication =>
              testAgentRegistrationConnector
                .findIndividuals(agentApplication.agentApplicationId)
                .map(individuals => (agentApplication, individuals))
          submittedRiskingResultsFilenames <- testRiskingService.listSubmittedRiskingResultsFilenames()
        yield Ok(showRecentApplicationsPage(
          applicationsWithIndividuals,
          submittedRiskingResultsFilenames,
          page,
          pageSize
        ))

  def showAgentApplicationById(agentApplicationId: AgentApplicationId): Action[AnyContent] =
    testOnlyActions
      .getApplication(agentApplicationId):
        implicit request: RequestWithData[TestOnlyActions.DataWithApplication] =>
          Ok(Json.prettyPrint(Json.toJson(request.agentApplication)))

  /** Kept only so old bookmarked/shared links keep working — resolves the application's reference and redirects to `showAgentApplicationDetailsByReference`,
    * which is now the actual details page.
    */
  def showAgentApplicationTile(agentApplicationId: AgentApplicationId): Action[AnyContent] =
    testOnlyActions
      .getApplication(agentApplicationId):
        implicit request: RequestWithData[TestOnlyActions.DataWithApplication] =>
          Redirect(AppRoutes.testOnly.applicant.TestOnlyController.showAgentApplicationDetailsByReference(request.agentApplication.applicationReference))

  def showAgentApplicationDetailsByReference(applicationReference: ApplicationReference): Action[AnyContent] = testOnlyActions
    .getApplication(applicationReference)
    .refine:
      implicit request: RequestWithData[TestOnlyActions.DataWithApplication] =>
        testAgentRegistrationConnector
          .findIndividuals(request.get[AgentApplication].agentApplicationId)
          .map(request.add)
    .async:
      implicit request: RequestWithData[List[IndividualProvidedDetails] *: TestOnlyActions.DataWithApplication] =>
        val agentApplication: AgentApplication = request.get[AgentApplication]
        val individuals: List[IndividualProvidedDetails] = request.get[List[IndividualProvidedDetails]]
        testRiskingService.listSubmittedRiskingResultsFilenames().map { submittedRiskingResultsFilenames =>
          Ok(showAgentApplicationsTilePage(
            agentApplication,
            individuals,
            submittedRiskingResultsFilenames
          ))
        }

  def showIndividualsForApplication: Action[AnyContent] = actions
    .getApplication
    .async:
      implicit request =>
        individualProvidedDetailsConnector
          .findAll(request.get[AgentApplication].agentApplicationId)
          .map: individuals =>
            Ok(Json.prettyPrint(Json.toJson(individuals)))

  def addAgentTypeToSession(
    agentType: AgentType
  ): Action[AnyContent] = actions.action:
    implicit request =>
      Ok("agent type added to session")
        .addToSession(agentType)

  def addBusinessTypeToSession(
    businessType: BusinessTypeAnswer
  ): Action[AnyContent] = actions.action:
    implicit request =>
      Ok("business type added to session")
        .addToSession(AgentType.UkTaxAgent)
        .addToSession(businessType)

  def addPartnershipTypeToSession(
    partnershipType: BusinessType.Partnership
  ): Action[AnyContent] = actions.action:
    implicit request =>
      Ok("partnership type added to session")
        .addToSession(AgentType.UkTaxAgent)
        .addToSession(BusinessTypeAnswer.PartnershipType)
        .addSession(partnershipType)

  def addUserRoleToSession(
    userRole: UserRole
  ): Action[AnyContent] = actions.action:
    implicit request =>
      Ok("user role added to session")
        .addToSession(AgentType.UkTaxAgent)
        .addToSession(BusinessTypeAnswer.LimitedCompany) // in specs we are always adding Director
        .addToSession(userRole)

  def addAgentApplicationIdToSession(
    agentApplicationId: AgentApplicationId
  ): Action[AnyContent] = actions.action:
    implicit request =>
      Ok("agent applicationId added to session")
        .addToSession(agentApplicationId)

  // TODO: remove this once FF links can handle this

  // as we add more types of entity support we may want to specify which business type to create
  // possibly as part of the url, for now we only create an LLP application
  def makeTestSubmittedApplication(): Action[AnyContent] = actions.action
    .async:
      implicit request =>
        testApplicationService
          .makeTestApplication()
          .map((linkId: TestOnlyLink) =>
            Ok(testLinkPage(linkId))
          )
