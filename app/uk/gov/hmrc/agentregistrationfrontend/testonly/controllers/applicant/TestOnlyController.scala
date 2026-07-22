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
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.EntityRiskingFailure
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.IndividualRiskingFailure
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessTypeAnswer
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.TestOnlyLink
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.TestApplicationService
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.TestRiskingService
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.ShowRecentApplicationsPage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.ShowAgentApplicationsTilePage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.SelectRiskingFailuresPage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.SelectIndividualRiskingFailuresPage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.TestLinkPage
import uk.gov.hmrc.agentregistrationfrontend.connectors.IndividualProvidedDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.testonly.connectors.TestAgentRegistrationConnector

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future
import scala.util.Random

@Singleton
class TestOnlyController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  testApplicationService: TestApplicationService,
  testRiskingService: TestRiskingService,
  testAgentRegistrationConnector: TestAgentRegistrationConnector,
  testLinkPage: TestLinkPage,
  showRecentApplicationsPage: ShowRecentApplicationsPage,
  showAgentApplicationsTilePage: ShowAgentApplicationsTilePage,
  selectRiskingFailuresPage: SelectRiskingFailuresPage,
  selectIndividualRiskingFailuresPage: SelectIndividualRiskingFailuresPage,
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
        for
          applications <- testAgentRegistrationConnector.getRecentApplications()
          applicationsWithIndividuals: Seq[(AgentApplication, List[IndividualProvidedDetails])] <- Future.sequence:
            applications.map: agentApplication =>
              testAgentRegistrationConnector
                .findIndividuals(agentApplication.agentApplicationId)
                .map(individuals => (agentApplication, individuals))
          submittedRiskingResultsFilenames <- testRiskingService.listSubmittedRiskingResultsFilenames()
        yield Ok(showRecentApplicationsPage(applicationsWithIndividuals, submittedRiskingResultsFilenames))

  def showAgentApplicationById(agentApplicationId: AgentApplicationId): Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        testAgentRegistrationConnector
          .findApplication(agentApplicationId)
          .map:
            case Some(application) => Ok(Json.prettyPrint(Json.toJson(application)))
            case None => Ok(s"No application with such id: $agentApplicationId")

  def showAgentApplicationTile(agentApplicationId: AgentApplicationId): Action[AnyContent] = actions
    .action
    .refine:
      implicit request =>
        testAgentRegistrationConnector.findApplication(agentApplicationId)
          .map[Result | RequestWithData[AgentApplication *: EmptyData]]:
            case Some(agentApplication) => request.add(agentApplication)
            case None => InternalServerError(s"There is no application under given agentApplicationId: $agentApplicationId")
    .refine:
      implicit request =>
        testAgentRegistrationConnector
          .findIndividuals(request.get[AgentApplication].agentApplicationId)
          .map[Result | RequestWithData[List[IndividualProvidedDetails] *: AgentApplication *: EmptyData]](request.add)
    .async:
      implicit request =>
        val agentApplication: AgentApplication = request.get[AgentApplication]
        val individuals: List[IndividualProvidedDetails] = request.get[List[IndividualProvidedDetails]]
        testRiskingService
          .listSubmittedRiskingResultsFilenames()
          .map: submittedRiskingResultsFilenames =>
            Ok(showAgentApplicationsTilePage(
              agentApplication,
              individuals,
              submittedRiskingResultsFilenames
            ))

  def selectRiskingFailures(agentApplicationId: AgentApplicationId): Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        testAgentRegistrationConnector
          .findApplication(agentApplicationId)
          .map:
            case Some(agentApplication) => Ok(selectRiskingFailuresPage(agentApplication))
            case None => InternalServerError(s"There is no application under given agentApplicationId: $agentApplicationId")

  def submitRiskingFailures(agentApplicationId: AgentApplicationId): Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        val formData: Map[String, Seq[String]] = request.body.asFormUrlEncoded.getOrElse(Map.empty)
        for
          maybeApplication <- testAgentRegistrationConnector.findApplication(agentApplicationId)
          agentApplication = maybeApplication.getOrElse(
            throw new RuntimeException(s"There is no application under given agentApplicationId: $agentApplicationId")
          )
          failures: Seq[EntityRiskingFailure] = formData.getOrElse("entityFailure", Seq.empty).flatMap(parseEntityFailure)
          _ <- testRiskingService.submitEntityFailures(agentApplication.applicationReference, failures)
        yield Redirect(AppRoutes.testOnly.applicant.TestOnlyController.showAgentApplicationTile(agentApplicationId))

  private def parseEntityFailure(value: String): Option[EntityRiskingFailure] = EntityRiskingFailure.values.find(_.toString === value)

  /** Quick action: submits with no failures at all, i.e. an Approved outcome, without having to manually leave every checkbox unticked. */
  def submitEntityFailuresApproved(
    agentApplicationId: AgentApplicationId,
    redirectUrl: String
  ): Action[AnyContent] = submitEntityFailuresQuickAction(
    agentApplicationId,
    redirectUrl,
    Seq.empty
  )

  /** Quick action: submits a random 1-3 fixable failures, i.e. a FailedFixable outcome. */
  def submitEntityFailuresFixable(
    agentApplicationId: AgentApplicationId,
    redirectUrl: String
  ): Action[AnyContent] = submitEntityFailuresQuickAction(
    agentApplicationId,
    redirectUrl,
    randomFixableEntityFailures(1 + Random.nextInt(3))
  )

  /** Quick action: submits a random mix of non-fixable (and possibly some fixable) failures, i.e. a FailedNonFixable outcome. */
  def submitEntityFailuresNonFixable(
    agentApplicationId: AgentApplicationId,
    redirectUrl: String
  ): Action[AnyContent] = submitEntityFailuresQuickAction(
    agentApplicationId,
    redirectUrl,
    randomNonFixableEntityFailures()
  )

  private def submitEntityFailuresQuickAction(
    agentApplicationId: AgentApplicationId,
    redirectUrl: String,
    failures: Seq[EntityRiskingFailure]
  ): Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        for
          maybeApplication <- testAgentRegistrationConnector.findApplication(agentApplicationId)
          agentApplication = maybeApplication.getOrElse(
            throw new RuntimeException(s"There is no application under given agentApplicationId: $agentApplicationId")
          )
          _ <- testRiskingService.submitEntityFailures(agentApplication.applicationReference, failures)
        yield Redirect(redirectUrl)

  /** A random selection of `count` fixable entity failures containing at most one AMLS (Check 3) failure — the real risking model only ever produces a single
    * AMLS fix per application, so picking from the raw fixable catalogue directly could easily select two or more AMLS checks and build test data that could
    * never occur for real.
    */
  private def randomFixableEntityFailures(count: Int): Seq[EntityRiskingFailure] =
    val allFixableFailures: Seq[EntityRiskingFailure] = EntityRiskingFailure.values.filter(_.fixable).toSeq
    val (amlsFailures, otherFixableFailures) = allFixableFailures.partition(_.checkId === "3")
    val pool = otherFixableFailures ++ Random.shuffle(amlsFailures).take(1)
    Random.shuffle(pool).take(count)

  /** A random selection guaranteed to contain at least one non-fixable entity check, plus a random 0-2 fixable checks (with the same at-most-one-AMLS rule as
    * `randomFixableEntityFailures`) bundled alongside it.
    */
  private def randomNonFixableEntityFailures(): Seq[EntityRiskingFailure] =
    val nonFixableFailures: Seq[EntityRiskingFailure] = EntityRiskingFailure.values.filterNot(_.fixable).toSeq
    val randomNonFixable = Random.shuffle(nonFixableFailures).take(1 + Random.nextInt(3))
    val randomFixable = randomFixableEntityFailures(Random.nextInt(3))
    randomNonFixable ++ randomFixable

  def selectIndividualRiskingFailures(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        testAgentRegistrationConnector
          .findIndividual(individualProvidedDetailsId)
          .map:
            case Some(individual) => Ok(selectIndividualRiskingFailuresPage(individual))
            case None => InternalServerError(s"There is no individual under given individualProvidedDetailsId: $individualProvidedDetailsId")

  def submitIndividualRiskingFailures(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        val formData: Map[String, Seq[String]] = request.body.asFormUrlEncoded.getOrElse(Map.empty)
        for
          maybeIndividual <- testAgentRegistrationConnector.findIndividual(individualProvidedDetailsId)
          individual = maybeIndividual.getOrElse(
            throw new RuntimeException(s"There is no individual under given individualProvidedDetailsId: $individualProvidedDetailsId")
          )
          failures: Seq[IndividualRiskingFailure] = formData.getOrElse("individualFailure", Seq.empty).flatMap(parseIndividualFailure)
          _ <- testRiskingService.submitIndividualFailures(individual.personReference, failures)
        yield Redirect(AppRoutes.testOnly.applicant.TestOnlyController.showAgentApplicationTile(individual.agentApplicationId))

  private def parseIndividualFailure(value: String): Option[IndividualRiskingFailure] = IndividualRiskingFailure.values.find(_.toString === value)

  /** Quick action: submits with no failures at all, i.e. an Approved outcome, without having to manually leave every checkbox unticked. */
  def submitIndividualFailuresApproved(
    individualProvidedDetailsId: IndividualProvidedDetailsId,
    redirectUrl: String
  ): Action[AnyContent] = submitIndividualFailuresQuickAction(
    individualProvidedDetailsId,
    redirectUrl,
    Seq.empty
  )

  /** Quick action: submits a random 1-3 fixable failures, i.e. a FailedFixable outcome. */
  def submitIndividualFailuresFixable(
    individualProvidedDetailsId: IndividualProvidedDetailsId,
    redirectUrl: String
  ): Action[AnyContent] = submitIndividualFailuresQuickAction(
    individualProvidedDetailsId,
    redirectUrl,
    randomFixableIndividualFailures()
  )

  /** Quick action: submits a random mix of non-fixable (and possibly some fixable) failures, i.e. a FailedNonFixable outcome. */
  def submitIndividualFailuresNonFixable(
    individualProvidedDetailsId: IndividualProvidedDetailsId,
    redirectUrl: String
  ): Action[AnyContent] = submitIndividualFailuresQuickAction(
    individualProvidedDetailsId,
    redirectUrl,
    randomNonFixableIndividualFailures()
  )

  private def submitIndividualFailuresQuickAction(
    individualProvidedDetailsId: IndividualProvidedDetailsId,
    redirectUrl: String,
    failures: Seq[IndividualRiskingFailure]
  ): Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        for
          maybeIndividual <- testAgentRegistrationConnector.findIndividual(individualProvidedDetailsId)
          individual = maybeIndividual.getOrElse(
            throw new RuntimeException(s"There is no individual under given individualProvidedDetailsId: $individualProvidedDetailsId")
          )
          _ <- testRiskingService.submitIndividualFailures(individual.personReference, failures)
        yield Redirect(redirectUrl)

  /** A random non-empty subset (1-3) of the fixable individual checks. */
  private def randomFixableIndividualFailures(): Seq[IndividualRiskingFailure] =
    val fixableFailures: Seq[IndividualRiskingFailure] = IndividualRiskingFailure.values.filter(_.fixable).toSeq
    Random.shuffle(fixableFailures).take(1 + Random.nextInt(3))

  /** A random selection guaranteed to contain at least one non-fixable individual check, plus a random 0-2 fixable checks bundled alongside it. */
  private def randomNonFixableIndividualFailures(): Seq[IndividualRiskingFailure] =
    val nonFixableFailures: Seq[IndividualRiskingFailure] = IndividualRiskingFailure.values.filterNot(_.fixable).toSeq
    val fixableFailures: Seq[IndividualRiskingFailure] = IndividualRiskingFailure.values.filter(_.fixable).toSeq
    val randomNonFixable = Random.shuffle(nonFixableFailures).take(1 + Random.nextInt(3))
    val randomFixable = Random.shuffle(fixableFailures).take(Random.nextInt(3))
    randomNonFixable ++ randomFixable

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

  def addUserRoleToSession(
    userRole: UserRole
  ): Action[AnyContent] = Action:
    implicit request =>
      Ok("user role added to session")
        .addToSession(AgentType.UkTaxAgent)
        .addToSession(BusinessTypeAnswer.LimitedCompany) // in specs we are always adding Director
        .addToSession(userRole)

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
