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

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsNotSoleTrader
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.RemoveKeyIndividualForm
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.otherrelevantindividuals.RemoveKeyIndividualPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class RemoveOtherRelevantIndividualController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: RemoveKeyIndividualPage,
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
        individualProvideDetailsService
          .findById(individualProvidedDetailsId)
          .map[RequestWithData[DataWithIndividual] | Result]:
            case Some(individualProvidedDetails) => request.add[IndividualProvidedDetails](individualProvidedDetails)
            case None =>
              logger.warn(
                "Number of required key individuals not specified in application, redirecting to number of key individuals page"
              )
              Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)

  def show(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] =
    baseAction(individualProvidedDetailsId):
      implicit request =>
        val existingRecord: IndividualProvidedDetails = request.get
        Ok(view(
          form = RemoveKeyIndividualForm.form(existingRecord.individualName.value),
          individualProvidedDetails = existingRecord
        ))

  def submit(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = baseAction(individualProvidedDetailsId)
    .ensureValidFormAndRedirectIfSaveForLater[YesNo](
      form =
        (request: RequestWithData[DataWithIndividual]) =>
          RemoveKeyIndividualForm.form(request.get[IndividualProvidedDetails].individualName.value),
      resultToServeWhenFormHasErrors =
        implicit request =>
          formWithErrors =>
            view(
              form = formWithErrors,
              individualProvidedDetails = request.get[IndividualProvidedDetails]
            )
    )
    .async:
      implicit request =>
        val confirmRemoveIndividual: YesNo = request.get
        val individualProvidedDetails: IndividualProvidedDetails = request.get
        confirmRemoveIndividual match
          case YesNo.Yes =>
            individualProvideDetailsService
              .delete(individualProvidedDetails._id)
              .map: _ =>
                Redirect(
                  AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show
                )
          case YesNo.No =>
            Future.successful(
              Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show)
            )
