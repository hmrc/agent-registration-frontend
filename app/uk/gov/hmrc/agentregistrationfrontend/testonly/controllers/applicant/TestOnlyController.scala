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
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessTypeAnswer
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.TestOnlyLink
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.TestApplicationService
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.ShowRecentApplicationsPage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.TestLinkPage
import uk.gov.hmrc.agentregistrationfrontend.connectors.AgentRegistrationConnector
import uk.gov.hmrc.agentregistrationfrontend.connectors.IndividualProvidedDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.testonly.connectors.TestAgentRegistrationConnector

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestOnlyController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  testApplicationService: TestApplicationService,
  testAgentRegistrationConnector: TestAgentRegistrationConnector,
  agentRegistrationConnector: AgentRegistrationConnector,
  testLinkPage: TestLinkPage,
  showRecentApplicationsPage: ShowRecentApplicationsPage,
  individualProvidedDetailsConnector: IndividualProvidedDetailsConnector
)
extends FrontendController(mcc, actions):

  def showAgentApplication: Action[AnyContent] = actions
    .getApplication: request =>
      Ok(Json.prettyPrint(Json.toJson(request.agentApplication)))

  def showRecentAgentApplications: Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        testAgentRegistrationConnector
          .getRecentApplications()
          .map(applications => Ok(showRecentApplicationsPage(applications)))

  def showAgentApplicationById(agentApplicationId: AgentApplicationId): Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        testAgentRegistrationConnector
          .findApplication(agentApplicationId)
          .map:
            case Some(application) => Ok(Json.prettyPrint(Json.toJson(application)))
            case None => Ok(s"No application with such id: $agentApplicationId")

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
  ): Action[AnyContent] = Action:
    implicit request =>
      Ok("agent type added to session")
        .addToSession(agentType)

  def addBusinessTypeToSession(
    businessType: BusinessTypeAnswer
  ): Action[AnyContent] = Action:
    implicit request =>
      Ok("business type added to session")
        .addToSession(AgentType.UkTaxAgent)
        .addToSession(businessType)

  def addPartnershipTypeToSession(
    partnershipType: BusinessType.Partnership
  ): Action[AnyContent] = Action:
    implicit request =>
      Ok("partnership type added to session")
        .addToSession(AgentType.UkTaxAgent)
        .addToSession(BusinessTypeAnswer.PartnershipType)
        .addSession(partnershipType)

  def addAgentApplicationIdToSession(
    agentApplicationId: AgentApplicationId
  ): Action[AnyContent] = Action:
    implicit request =>
      Ok("agent applicationId added to session")
        .addToSession(agentApplicationId)

  // TODO: remove this once FF links can handle this

  // as we add more types of entity support we may want to specify which business type to create
  // possibly as part of the url, for now we only create an LLP application
  def makeTestSubmittedApplication(): Action[AnyContent] = Action
    .async:
      implicit request =>
        testApplicationService
          .makeTestApplication()
          .map((linkId: TestOnlyLink) =>
            Ok(testLinkPage(linkId))
          )
