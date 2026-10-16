/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.otherrelevantindividuals

import com.softwaremill.quicklens.modify
import play.api.data.Form
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsNotSoleTrader

import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.OtherRelevantIndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.otherrelevantindividuals.EnterIndividualNamePage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class ChangeOtherRelevantIndividualController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  enterIndividualNamePage: EnterIndividualNamePage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private type DataWithIndividual = IndividualProvidedDetails *: IsNotSoleTrader *: DataWithAuth

  private def baseAction(individualProvidedDetailsId: IndividualProvidedDetailsId): ActionBuilderWithData[DataWithIndividual] = actions
    .getApplicationInProgress
    .refine:
      implicit request =>
        request.get[AgentApplication] match
          case _: AgentApplicationSoleTrader =>
            logger.warn("Sole traders do not add other relevant individuals to a list, redirecting to task list for the correct links")
            Redirect(AppRoutes.apply.TaskListController.show.url)
          case aa: IsNotSoleTrader => request.replace[AgentApplication, IsNotSoleTrader](aa)
    .refine:
      implicit request =>
        request.get[IsNotSoleTrader].hasOtherRelevantIndividuals match
          case Some(true) => request
          case Some(false) => Redirect(AppRoutes.apply.listdetails.CheckYourAnswersController.show.url)
          case None =>
            logger.warn(
              "Other relevant individuals not specified in application,  redirecting to confirm if they are any other relevant individual page"
            )
            Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.show.url)
    .refine:
      implicit request =>
        val agentApplication: IsNotSoleTrader = request.get
        individualProvideDetailsService
          .findAllOtherRelevantIndividualsByApplicationId(agentApplication.agentApplicationId)
          .map(_.find(_._id === individualProvidedDetailsId))
          .map[RequestWithData[DataWithIndividual] | Result]:
            case Some(individualProvidedDetails) => request.add[IndividualProvidedDetails](individualProvidedDetails)
            case None =>
              logger.warn(
                s"Individual provided details record not found for the provided id ${individualProvidedDetailsId.value} - redirecting as this may be due to a stale link or an attempt to access another record that does not belong to the current application"
              )
              Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url)

  def show(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = baseAction(individualProvidedDetailsId)
    .async:
      implicit request =>
        val formAction: Call = AppRoutes.apply.listdetails.otherrelevantindividuals.ChangeOtherRelevantIndividualController.submit(
          individualProvidedDetailsId
        )
        val nameToChange: IndividualName = request.get[IndividualProvidedDetails].individualName

        Future.successful(Ok(enterIndividualNamePage(
          form = OtherRelevantIndividualNameForm.form.fill(nameToChange),
          formAction = formAction,
          ordinalKey = "change",
          agentApplication = request.get[IsNotSoleTrader]
        )))

  def submit(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] =
    baseAction(individualProvidedDetailsId)
      .ensureValidFormAndRedirectIfSaveForLater[IndividualName](
        form = OtherRelevantIndividualNameForm.form,
        resultToServeWhenFormHasErrors =
          implicit request =>
            (formWithErrors: Form[IndividualName]) =>
              val formAction: Call = AppRoutes.apply.listdetails.otherrelevantindividuals.ChangeOtherRelevantIndividualController.submit(
                individualProvidedDetailsId
              )

              BadRequest(
                enterIndividualNamePage(
                  form = formWithErrors,
                  formAction = formAction,
                  ordinalKey = "change",
                  agentApplication = request.get[IsNotSoleTrader]
                )
              )
      )
      .async:
        implicit request =>
          val individualNameFromForm: IndividualName = request.get
          val individualToChange: IndividualProvidedDetails = request.get
          individualProvideDetailsService.upsertForApplication(
            individualToChange
              .modify(_.individualName)
              .setTo(individualNameFromForm)
          )
            .map: _ =>
              Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show)
      .redirectIfSaveForLater
