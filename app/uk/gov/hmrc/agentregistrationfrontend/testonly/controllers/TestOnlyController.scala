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
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendControllerBase
import uk.gov.hmrc.agentregistrationfrontend.testonly.forms.SelectEntityFailuresForm
import uk.gov.hmrc.agentregistrationfrontend.testonly.forms.SelectIndividualFailuresForm
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.PlanetId
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.UploadRiskingResultsFileOutcome
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.UserId
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.StubUserService
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.TestApplicationService
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.TestRiskingService
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.TestOnlyHubPage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.ResetDatabaseConfirmationPage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.RiskingActionConfirmationPage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.SelectEntityFailuresPage
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.SelectIndividualFailuresPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

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

  def showSelectEntityFailures(applicationReference: ApplicationReference): Action[AnyContent] = defaultActionBuilder:
    implicit request =>
      Ok(selectEntityFailuresPage(applicationReference, SelectEntityFailuresForm.form))

  def submitEntityFailures(applicationReference: ApplicationReference): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      SelectEntityFailuresForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(selectEntityFailuresPage(applicationReference, formWithErrors))),
          failures =>
            testRiskingService.submitEntityFailures(applicationReference, failures).map:
              case UploadRiskingResultsFileOutcome.Uploaded =>
                Ok(riskingActionConfirmationPage(
                  heading = "Risking results submitted",
                  description =
                    s"A risking results file has been uploaded for application reference ${applicationReference.value}. " +
                      "It won't take effect until results file processing is run."
                ))
              case UploadRiskingResultsFileOutcome.AlreadyExists =>
                val formWithError = SelectEntityFailuresForm.form
                  .fill(failures)
                  .withGlobalError(s"Risking results have already been submitted for application reference ${applicationReference.value}.")
                Conflict(selectEntityFailuresPage(applicationReference, formWithError))
        )

  def showSelectIndividualFailures(personReference: PersonReference): Action[AnyContent] = defaultActionBuilder:
    implicit request =>
      Ok(selectIndividualFailuresPage(personReference, SelectIndividualFailuresForm.form))

  def submitIndividualFailures(personReference: PersonReference): Action[AnyContent] = defaultActionBuilder.async:
    implicit request =>
      SelectIndividualFailuresForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(selectIndividualFailuresPage(personReference, formWithErrors))),
          failures =>
            testRiskingService.submitIndividualFailures(personReference, failures).map:
              case UploadRiskingResultsFileOutcome.Uploaded =>
                Ok(riskingActionConfirmationPage(
                  heading = "Risking results submitted",
                  description =
                    s"A risking results file has been uploaded for person reference ${personReference.value}. " +
                      "It won't take effect until results file processing is run."
                ))
              case UploadRiskingResultsFileOutcome.AlreadyExists =>
                val formWithError = SelectIndividualFailuresForm.form
                  .fill(failures)
                  .withGlobalError(s"Risking results have already been submitted for person reference ${personReference.value}.")
                Conflict(selectIndividualFailuresPage(personReference, formWithError))
        )
