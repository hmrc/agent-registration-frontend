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

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.DefaultActionBuilder
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendControllerBase
import uk.gov.hmrc.agentregistrationfrontend.testonly.forms.SelectEntityFailuresForm
import uk.gov.hmrc.agentregistrationfrontend.testonly.forms.SelectIndividualFailuresForm
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.EntityRiskingFailure
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.IndividualRiskingFailure
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.PlanetId
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.UserId
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.StubUserService
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.TestApplicationService
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.TestRiskingService
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.TestOnlyHubPage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.ResetDatabaseConfirmationPage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.RiskingActionConfirmationPage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.SelectEntityFailuresPage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.SelectIndividualFailuresPage

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future
import scala.util.Random

@Singleton
class TestOnlyController @Inject() (
  mcc: MessagesControllerComponents,
  defaultActionBuilder: DefaultActionBuilder,
  testOnlyHubPage: TestOnlyHubPage,
  resetDatabaseConfirmationPage: ResetDatabaseConfirmationPage,
  riskingActionConfirmationPage: RiskingActionConfirmationPage,
  selectEntityFailuresPage: SelectEntityFailuresPage,
  selectIndividualFailuresPage: SelectIndividualFailuresPage,
  stubUserService: StubUserService,
  testApplicationService: TestApplicationService,
  testRiskingService: TestRiskingService,
  appConfig: AppConfig
)
extends FrontendControllerBase(mcc):

  def showTestOnlyHub: Action[AnyContent] = defaultActionBuilder:
    implicit request =>
      Ok(testOnlyHubPage())

  def showPlaySession: Action[AnyContent] = defaultActionBuilder: request =>
    Ok(Json.prettyPrint(Json.toJson(request.session.data)))

  def findAndLogInApplicant(
    userId: UserId,
    planetId: PlanetId,
    redirectUrl: String
  ): Action[AnyContent] = defaultActionBuilder
    .async:
      implicit request =>

        import StubUserService.addToSession
        for
          user <- stubUserService.findUser(userId, planetId).map(_.getOrThrowExpectedDataMissing("user"))
          loginResponse <- stubUserService.signIn(user)
        yield Redirect(redirectUrl).addToSession(loginResponse)

  def findOrCreateAndLogInIndividual(
    userId: UserId,
    planetId: PlanetId,
    individualName: String,
    redirectUrl: String
  ): Action[AnyContent] = defaultActionBuilder
    .async:
      implicit request =>
        import StubUserService.addToSession
        for
          maybeUser <- stubUserService.findUser(userId, planetId)
          user <-
            maybeUser match
              case Some(user) => Future.successful(user)
              case None =>
                stubUserService.createUserIndividual(
                  userId,
                  planetId,
                  individualName
                )
          loginResponse <- stubUserService.signIn(user)
        yield Redirect(redirectUrl).addToSession(loginResponse)

  def showResetDatabaseConfirmation: Action[AnyContent] = defaultActionBuilder:
    implicit request =>
      if appConfig.TestOnly.allowResetDatabase
      then Ok(resetDatabaseConfirmationPage())
      else Unauthorized("Reset operation not allowed")

  def resetDatabase: Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      if (appConfig.TestOnly.allowResetDatabase)
        for
          _ <- testApplicationService.deleteAll()
          _ <- testRiskingService.deleteAll()
        yield Redirect(AppRoutes.testOnly.TestOnlyController.showTestOnlyHub)
      else
        Future.successful(Unauthorized("Reset operation not allowed"))

  def runRisking: Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      testRiskingService.runRisking().map: _ =>
        Ok(riskingActionConfirmationPage(
          heading = "Risking scheduled",
          description =
            "This collects every application that's in the SentForRisking state, together with their individuals, builds a pipe-separated" +
              " risking file, and sends it to Minerva. On success, those applications move to the SentToMinerva state. Processing happens" +
              " asynchronously in the background."
        ))

  /** Same underlying action as `runRisking`, but for the quick-nav link: redirects back to the page the user was on instead of showing the confirmation page,
    * so it can be used as a one-click action from anywhere in the test-only tooling.
    */
  def runRiskingAndRedirect(redirectUrl: String): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      testRiskingService.runRisking().map: _ =>
        Redirect(redirectUrl)

  def runResultsFileProcessing: Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      testRiskingService.runResultsFileProcessing().map: _ =>
        Ok(riskingActionConfirmationPage(
          heading = "Results file processing scheduled",
          description =
            "This picks up risking results files uploaded via the SDES test-only endpoint (for example via 'Select entity failures' /" +
              " 'Select individual failures' on this hub), parses the records in them, and applies the outcomes to the matching" +
              " applications/individuals in agent-registration-risking. This is what would normally happen when Minerva sends back" +
              " risking decisions. Once fully processed, an application moves to the RiskingCompleted state and its records are archived" +
              " into the completed-risking collection. Processing happens asynchronously in the background."
        ))

  /** Same underlying action as `runResultsFileProcessing`, but for the quick-nav link: redirects back to the page the user was on instead of showing the
    * confirmation page, so it can be used as a one-click action from anywhere in the test-only tooling.
    */
  def runResultsFileProcessingAndRedirect(redirectUrl: String): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      testRiskingService.runResultsFileProcessing().map: _ =>
        Redirect(redirectUrl)

  def viewNextRiskingFileContents: Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      testRiskingService.viewNextRiskingFileContents().map(Ok(_))

  def showRiskingResultsFile(filename: String): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      testRiskingService.viewRiskingResultsFile(filename).map:
        case Some(content) => Ok(content)
        case None => Ok(s"No risking results file found for filename: $filename")

  def showApplicationForRisking(applicationReference: ApplicationReference): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      testRiskingService.findApplicationForRisking(applicationReference).map:
        case Some(json) => Ok(Json.prettyPrint(json))
        case None => Ok(s"No application-for-risking found for applicationReference: ${applicationReference.value}")

  def showIndividualsForRisking(applicationReference: ApplicationReference): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      testRiskingService.findIndividualsForRisking(applicationReference).map:
        case Some(json) => Ok(Json.prettyPrint(json))
        case None => Ok(s"No individuals-for-risking found for applicationReference: ${applicationReference.value}")

  def showIndividualForRisking(personReference: PersonReference): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      testRiskingService.findIndividualForRisking(personReference).map:
        case Some(json) => Ok(Json.prettyPrint(json))
        case None => Ok(s"No individual-for-risking found for personReference: ${personReference.value}")

  def showCompletedRisking(applicationReference: ApplicationReference): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      testRiskingService.findCompletedRisking(applicationReference).map:
        case Some(json) => Ok(Json.prettyPrint(json))
        case None => Ok(s"No completed risking found for applicationReference: ${applicationReference.value}")

  def showSelectEntityFailures(
    applicationReference: ApplicationReference,
    reSubmittedAt: Option[Long],
    redirectUrl: String
  ): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      testApplicationService.findApplication(applicationReference).map: maybeAgentApplication =>
        val agentApplication = maybeAgentApplication.getOrThrowExpectedDataMissing("agentApplication")
        Ok(selectEntityFailuresPage(
          applicationReference,
          reSubmittedAt.map(Instant.ofEpochMilli),
          redirectUrl,
          SelectEntityFailuresForm(agentApplication)
        ))

  def submitEntityFailures(
    applicationReference: ApplicationReference,
    reSubmittedAt: Option[Long],
    redirectUrl: String
  ): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      val reSubmittedAtInstant = reSubmittedAt.map(Instant.ofEpochMilli)
      testApplicationService.findApplication(applicationReference).flatMap: maybeAgentApplication =>
        val agentApplication = maybeAgentApplication.getOrThrowExpectedDataMissing("agentApplication")
        SelectEntityFailuresForm(agentApplication)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(BadRequest(selectEntityFailuresPage(
                applicationReference,
                reSubmittedAtInstant,
                redirectUrl,
                formWithErrors
              ))),
            failures =>
              // Uploaded or AlreadyExists — either way, the results file exists now, so just go back to the application details page.
              testRiskingService.submitEntityFailures(
                applicationReference,
                failures,
                reSubmittedAtInstant
              ).map(_ => Redirect(redirectUrl))
          )

  /** Quick action: submits with no failures at all, i.e. an Approved outcome, without having to manually leave every checkbox unticked. Redirects back to
    * `redirectUrl` (the page the link was clicked from) instead of showing a confirmation page.
    */
  def submitEntityFailuresApproved(
    applicationReference: ApplicationReference,
    redirectUrl: String,
    reSubmittedAt: Option[Long]
  ): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      submitEntityFailuresQuickAction(
        applicationReference,
        Seq.empty,
        redirectUrl,
        reSubmittedAt
      )

  /** Quick action: submits a single fixable failure, i.e. a FailedFixable outcome. */
  def submitEntityFailuresFixable(
    applicationReference: ApplicationReference,
    redirectUrl: String,
    reSubmittedAt: Option[Long]
  ): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      testApplicationService.findApplication(applicationReference).flatMap: maybeAgentApplication =>
        val agentApplication = maybeAgentApplication.getOrThrowExpectedDataMissing("agentApplication")
        submitEntityFailuresQuickAction(
          applicationReference,
          randomFixableEntityFailures(1 + Random.nextInt(3), agentApplication),
          redirectUrl,
          reSubmittedAt
        )

  /** Quick action: submits a mix of one fixable and one non-fixable failure, i.e. a FailedNonFixable outcome whose failures still include a fixable one bundled
    * alongside the blocking one.
    */
  def submitEntityFailuresMixed(
    applicationReference: ApplicationReference,
    redirectUrl: String,
    reSubmittedAt: Option[Long]
  ): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      testApplicationService.findApplication(applicationReference).flatMap: maybeAgentApplication =>
        val agentApplication = maybeAgentApplication.getOrThrowExpectedDataMissing("agentApplication")
        submitEntityFailuresQuickAction(
          applicationReference,
          randomNonFixableEntityFailures(agentApplication),
          redirectUrl,
          reSubmittedAt
        )

  private def submitEntityFailuresQuickAction(
    applicationReference: ApplicationReference,
    failures: Seq[EntityRiskingFailure],
    redirectUrl: String,
    reSubmittedAt: Option[Long]
  )(using RequestHeader): Future[Result] =
    // Uploaded or AlreadyExists — either way, the results file exists now, so just go back to where the link was clicked from.
    testRiskingService.submitEntityFailures(
      applicationReference,
      failures,
      reSubmittedAt.map(Instant.ofEpochMilli)
    ).map(_ => Redirect(redirectUrl))

  /** A random non-empty subset (1 to 3) of the fixable entity checks, so repeated clicks of the "fail-fixable" quick action produce varied test data instead of
    * always the exact same single failure.
    */
  /** A random selection of `count` fixable entity failures containing at most one AMLS (Check 3) failure — the real risking model only ever produces a single
    * `EntityFix._3.AmlsFix` per application (see `SelectEntityFailuresForm`'s equivalent validation), so picking from the raw fixable catalogue directly could
    * easily select two or more AMLS checks and build test data that could never occur for real. Also excludes checks 4.2/5.2 (Corporation Tax) for sole
    * traders, matching `SelectEntityFailuresForm`'s validation rule.
    */
  private def randomFixableEntityFailures(
    count: Int,
    agentApplication: AgentApplication
  ): Seq[EntityRiskingFailure] =
    val allFixableFailures: Seq[EntityRiskingFailure] = EntityRiskingFailure.values.filter(_.fixable).toSeq
    val applicableFailures =
      if agentApplication.businessType === BusinessType.SoleTrader
      then allFixableFailures.filterNot(SelectEntityFailuresForm.soleTraderInapplicableFailures.contains)
      else allFixableFailures
    val (amlsFailures, otherFixableFailures) = applicableFailures.partition(_.checkId === "3")
    val pool = otherFixableFailures ++ Random.shuffle(amlsFailures).take(1)
    Random.shuffle(pool).take(count)

  /** A random selection guaranteed to contain at least one non-fixable entity check (so the resulting outcome is genuinely FailedNonFixable), plus a random 0-2
    * fixable checks (with the same at-most-one-AMLS rule as `randomFixableEntityFailures`) bundled alongside it — exercising the "Failures" + "Fixes" split
    * rendering on a genuinely mixed set.
    */
  private def randomNonFixableEntityFailures(agentApplication: AgentApplication): Seq[EntityRiskingFailure] =
    val nonFixableFailures: Seq[EntityRiskingFailure] = EntityRiskingFailure.values.filterNot(_.fixable).toSeq
    val randomNonFixable = Random.shuffle(nonFixableFailures).take(1 + Random.nextInt(3))
    val randomFixable = randomFixableEntityFailures(Random.nextInt(3), agentApplication)
    randomNonFixable ++ randomFixable

  def showSelectIndividualFailures(
    personReference: PersonReference,
    reSubmittedAt: Option[Long],
    redirectUrl: String
  ): Action[AnyContent] = defaultActionBuilder:
    implicit request =>
      Ok(selectIndividualFailuresPage(
        personReference,
        reSubmittedAt.map(Instant.ofEpochMilli),
        redirectUrl,
        SelectIndividualFailuresForm.form
      ))

  def submitIndividualFailures(
    personReference: PersonReference,
    reSubmittedAt: Option[Long],
    redirectUrl: String
  ): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      val reSubmittedAtInstant = reSubmittedAt.map(Instant.ofEpochMilli)
      SelectIndividualFailuresForm.form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(selectIndividualFailuresPage(
              personReference,
              reSubmittedAtInstant,
              redirectUrl,
              formWithErrors
            ))),
          failures =>
            // Uploaded or AlreadyExists — either way, the results file exists now, so just go back to the application details page.
            testRiskingService.submitIndividualFailures(
              personReference,
              failures,
              reSubmittedAtInstant
            ).map(_ => Redirect(redirectUrl))
        )

  /** Quick action from `SelectIndividualFailuresPage`: submits with no failures at all, i.e. an Approved outcome, without having to manually leave every
    * checkbox unticked. Redirects back to `redirectUrl` (the page the link was clicked from) instead of showing a confirmation page.
    */
  def submitIndividualFailuresApproved(
    personReference: PersonReference,
    redirectUrl: String,
    reSubmittedAt: Option[Long]
  ): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      submitIndividualFailuresQuickAction(
        personReference,
        Seq.empty,
        redirectUrl,
        reSubmittedAt
      )

  /** Quick action from `SelectIndividualFailuresPage`: submits a single fixable failure, i.e. a FailedFixable outcome. */
  def submitIndividualFailuresFixable(
    personReference: PersonReference,
    redirectUrl: String,
    reSubmittedAt: Option[Long]
  ): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      submitIndividualFailuresQuickAction(
        personReference,
        randomFixableIndividualFailures(),
        redirectUrl,
        reSubmittedAt
      )

  /** Quick action from `SelectIndividualFailuresPage`: submits a mix of one fixable and one non-fixable failure, i.e. a FailedNonFixable outcome whose failures
    * still include a fixable one bundled alongside the blocking one.
    */
  def submitIndividualFailuresMixed(
    personReference: PersonReference,
    redirectUrl: String,
    reSubmittedAt: Option[Long]
  ): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      submitIndividualFailuresQuickAction(
        personReference,
        randomNonFixableIndividualFailures(),
        redirectUrl,
        reSubmittedAt
      )

  private def submitIndividualFailuresQuickAction(
    personReference: PersonReference,
    failures: Seq[IndividualRiskingFailure],
    redirectUrl: String,
    reSubmittedAt: Option[Long]
  )(using RequestHeader): Future[Result] =
    // Uploaded or AlreadyExists — either way, the results file exists now, so just go back to where the link was clicked from.
    testRiskingService.submitIndividualFailures(
      personReference,
      failures,
      reSubmittedAt.map(Instant.ofEpochMilli)
    ).map(_ => Redirect(redirectUrl))

  /** A random non-empty subset (1 to 3) of the fixable individual checks, so repeated clicks of the "fail-fixable" quick action produce varied test data
    * instead of always the exact same single failure.
    */
  private def randomFixableIndividualFailures(): Seq[IndividualRiskingFailure] =
    val fixableFailures: Seq[IndividualRiskingFailure] = IndividualRiskingFailure.values.filter(_.fixable).toSeq
    Random.shuffle(fixableFailures).take(1 + Random.nextInt(3))

  /** A random selection guaranteed to contain at least one non-fixable individual check (so the resulting outcome is genuinely FailedNonFixable), plus a random
    * 0-2 fixable checks bundled alongside it — exercising the "Failures" + "Fixes" split rendering on a genuinely mixed set.
    */
  private def randomNonFixableIndividualFailures(): Seq[IndividualRiskingFailure] =
    val nonFixableFailures: Seq[IndividualRiskingFailure] = IndividualRiskingFailure.values.filterNot(_.fixable).toSeq
    val fixableFailures: Seq[IndividualRiskingFailure] = IndividualRiskingFailure.values.filter(_.fixable).toSeq
    val randomNonFixable = Random.shuffle(nonFixableFailures).take(1 + Random.nextInt(3))
    val randomFixable = Random.shuffle(fixableFailures).take(Random.nextInt(3))
    randomNonFixable ++ randomFixable
