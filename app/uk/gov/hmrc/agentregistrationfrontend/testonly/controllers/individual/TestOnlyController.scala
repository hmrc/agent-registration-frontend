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

package uk.gov.hmrc.agentregistrationfrontend.testonly.controllers.individual

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.testonly.connectors.TestAgentRegistrationConnector
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.PlanetId
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.User
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.UserId
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.StubUserService

import javax.inject.Inject
import javax.inject.Singleton
import StubUserService.addToSession

import scala.concurrent.Future

@Singleton
class TestOnlyController @Inject() (
  mcc: MessagesControllerComponents,
  actions: IndividualActions,
  individualProvideDetailsService: IndividualProvideDetailsService,
  testAgentRegistrationConnector: TestAgentRegistrationConnector,
  agentApplicationService: AgentApplicationService,
  stubUserService: StubUserService
)
extends FrontendController(mcc, actions):

  private def baseAction(linkId: LinkId): ActionBuilderWithData[DataWithIndividualProvidedDetails] = actions
    .authorised
    .refine(implicit request =>
      agentApplicationService
        .find(linkId)
        .map:
          case Some(agentApplication) => request.add(agentApplication)
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
                Redirect(AppRoutes.providedetails.ExitController.genericExitPage.url)
              )
    )

  def showProvidedDetails(linkId: LinkId): Action[AnyContent] =
    baseAction(linkId):
      implicit request =>
        Ok(Json.prettyPrint(Json.toJson(request.get[IndividualProvidedDetails])))

  def showIndividualProvidedDetails(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        testAgentRegistrationConnector.findIndividual(individualProvidedDetailsId).map:
          case Some(individualProvidedDetails) => Ok(Json.prettyPrint(Json.toJson(individualProvidedDetails)))
          case None => Ok("There is no individual under the given ID")

  def createIndividualIfNeededAndLoginViaLinkId(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = actions
    .action
    .refine:
      implicit request =>
        testAgentRegistrationConnector.findIndividual(individualProvidedDetailsId)
          .map[Result | RequestWithData[IndividualProvidedDetails *: EmptyData]]:
            case Some(individualProvidedDetails) => request.add(individualProvidedDetails)
            case None => BadRequest(s"There is no individual under given id: $individualProvidedDetailsId")
    .refine:
      implicit request =>
        testAgentRegistrationConnector.findApplication(request.get[IndividualProvidedDetails].agentApplicationId)
          .map[Result | RequestWithData[AgentApplication *: IndividualProvidedDetails *: EmptyData]]:
            case Some(agentApplication) => request.add(agentApplication)
            case None => InternalServerError(s"There is no application under given individual id: $individualProvidedDetailsId")
    .async:
      implicit request =>
        val agentApplication: AgentApplication = request.get[AgentApplication]
        val url = AppRoutes.providedetails.StartController.start(agentApplication.linkId)
        val individualProvidedDetails = request.get[IndividualProvidedDetails]
        val userId: UserId = UserId.make(individualProvidedDetails.individualProvidedDetailsId)
        val planetId: PlanetId = PlanetId.make(agentApplication.agentApplicationId)

        for
          maybeUser <- stubUserService.findUser(userId, planetId)
          user: User <- maybeUser
            .map(Future.successful)
            .getOrElse(stubUserService.createIndividualUser(
              userId = userId,
              planetId = planetId
            ))
          loginResponse <- stubUserService.signIn(user)
        yield Redirect(url).addToSession(loginResponse)

  def addIndividualNameToSession(individualName: String): Action[AnyContent] = Action:
    implicit request =>
      Ok("individual name added to session").addingToSession(
        "agent-registration-frontend.individualName" -> individualName
      )
