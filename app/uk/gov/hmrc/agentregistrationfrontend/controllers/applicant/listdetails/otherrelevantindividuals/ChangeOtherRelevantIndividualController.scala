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
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsAgentApplicationForDeclaringNumberOfOtherRelevantIndividuals
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsIncorporated
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualNameForm
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

  private type DataWithList = List[IndividualProvidedDetails] *: IsAgentApplicationForDeclaringNumberOfOtherRelevantIndividuals *: DataWithAuth

  private val baseAction: ActionBuilderWithData[DataWithList] = actions
    .getApplicationInProgress
    .refine:
      implicit request =>
        request.get[AgentApplication] match
          case _: IsIncorporated =>
            logger.warn(
              "Incorporated businesses should be name matching key individuals against Companies House results, redirecting to task list for the correct links"
            )
            Redirect(AppRoutes.apply.TaskListController.show.url)
          case _: AgentApplicationSoleTrader =>
            logger.warn("Sole traders do not add individuals to a list, redirecting to task list for the correct links")
            Redirect(AppRoutes.apply.TaskListController.show.url)
          case aa: IsAgentApplicationForDeclaringNumberOfOtherRelevantIndividuals =>
            request.replace[AgentApplication, IsAgentApplicationForDeclaringNumberOfOtherRelevantIndividuals](aa)
    .refine:
      implicit request =>
        request.get[IsAgentApplicationForDeclaringNumberOfOtherRelevantIndividuals].hasOtherRelevantIndividuals match
          case Some(true) => request
          case Some(false) => Redirect(AppRoutes.apply.listdetails.CheckYourAnswersController.show.url)
          case None =>
            logger.warn(
              "Other relevant individuals not specified in application,  redirecting to confirm if they are any other relevant individual page"
            )
            Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.show.url)
    .refine:
      implicit request =>
        val agentApplication: IsAgentApplicationForDeclaringNumberOfOtherRelevantIndividuals = request.get
        individualProvideDetailsService.findAllOtherRelevantIndividualsByApplicationId(agentApplication.agentApplicationId).map: individualsList =>
          request.add[List[IndividualProvidedDetails]](individualsList)

  def show(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = baseAction
    .async:
      implicit request =>
        val existingList: List[IndividualProvidedDetails] = request.get
        val formAction: Call = AppRoutes.apply.listdetails.otherrelevantindividuals.ChangeOtherRelevantIndividualController.submit(
          individualProvidedDetailsId
        )
        val nameToChange: IndividualName =
          existingList
            .find(_._id === individualProvidedDetailsId)
            .getOrThrowExpectedDataMissing(
              s"IndividualProvidedDetails with id $individualProvidedDetailsId not found"
            )
            .individualName

        Future.successful(Ok(enterIndividualNamePage(
          form = IndividualNameForm.form.fill(nameToChange),
          formAction = formAction
        )))

  def submit(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = baseAction
    .ensureValidFormAndRedirectIfSaveForLater[IndividualName](
      form = IndividualNameForm.form,
      resultToServeWhenFormHasErrors =
        implicit request =>
          (formWithErrors: Form[IndividualName]) =>
            val formAction: Call = AppRoutes.apply.listdetails.otherrelevantindividuals.ChangeOtherRelevantIndividualController.submit(
              individualProvidedDetailsId
            )

            BadRequest(
              enterIndividualNamePage(
                form = formWithErrors,
                formAction = formAction
              )
            )
    )
    .async:
      implicit request =>
        val individualNameFromForm: IndividualName = request.get
        val existingList: List[IndividualProvidedDetails] = request.get
        val individualToChange: IndividualProvidedDetails = existingList
          .find(_._id === individualProvidedDetailsId)
          .getOrThrowExpectedDataMissing(
            s"IndividualProvidedDetails with id $individualProvidedDetailsId not found"
          )
        individualProvideDetailsService.upsert(
          individualToChange
            .modify(_.individualName)
            .setTo(individualNameFromForm)
        )
          .map: _ =>
            Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show)
