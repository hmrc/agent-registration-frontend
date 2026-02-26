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

import play.api.data.Form
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsNotSoleTrader
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.OtherRelevantIndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.MessageKeys
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.otherrelevantindividuals.EnterIndividualNamePage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class EnterOtherRelevantIndividualController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  enterIndividualNameSimplePage: EnterIndividualNamePage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private type DataWithList = List[IndividualProvidedDetails] *: IsNotSoleTrader *: DataWithAuth

  private val baseAction: ActionBuilderWithData[DataWithList] = actions
    .getApplicationInProgress
    .refine:
      implicit request =>
        request.get[AgentApplication] match
          case _: AgentApplicationSoleTrader =>
            logger.warn("Sole traders do not add other relevant individuals, redirecting to task list for the correct links")
            Redirect(AppRoutes.apply.TaskListController.show.url)
          case aa: IsNotSoleTrader => request.replace[AgentApplication, IsNotSoleTrader](aa)
    .refine:
      implicit request =>
        request.get[IsNotSoleTrader].hasOtherRelevantIndividuals match
          case Some(true) => request
          case Some(false) => Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url)
          case None => Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.show.url)
    .refine:
      implicit request =>
        val agentApplication: IsNotSoleTrader = request.get
        individualProvideDetailsService.findAllByApplicationId(agentApplication.agentApplicationId).map: individualsList =>
          request.add[List[IndividualProvidedDetails]](individualsList)

  def show: Action[AnyContent] = baseAction
    .async:
      implicit request: RequestWithData[DataWithList] =>
        val formAction: Call = AppRoutes.apply.listdetails.otherrelevantindividuals.EnterOtherRelevantIndividualController.submit
        Future.successful(Ok(enterIndividualNameSimplePage(
          form = OtherRelevantIndividualNameForm.form,
          formAction = formAction,
          ordinalKey = MessageKeys.ordinalKey(
            existingSize =
              request.get[List[IndividualProvidedDetails]]
                .filterNot(_.isPersonOfControl)
                .size,
            isOnlyOne = false
          )
        )))

  def submit: Action[AnyContent] = baseAction
    .ensureValidFormAndRedirectIfSaveForLater[IndividualName](
      form = OtherRelevantIndividualNameForm.form,
      resultToServeWhenFormHasErrors =
        implicit request =>
          (formWithErrors: Form[IndividualName]) =>
            val formAction: Call = AppRoutes.apply.listdetails.otherrelevantindividuals.EnterOtherRelevantIndividualController.submit
            Future.successful(BadRequest(enterIndividualNameSimplePage(
              form = formWithErrors,
              formAction = formAction,
              ordinalKey = MessageKeys.ordinalKey(
                existingSize =
                  request.get[List[IndividualProvidedDetails]]
                    .filterNot(_.isPersonOfControl)
                    .size,
                isOnlyOne = false
              )
            )))
    )
    .async:
      implicit request =>
        val individualName: IndividualName = request.get
        individualProvideDetailsService.upsertForApplication(individualProvideDetailsService.create(
          individualName = individualName,
          isPersonOfControl = false, // from this page we are only adding other relevant people, who are not persons of control
          agentApplicationId = request.get[IsNotSoleTrader].agentApplicationId
        ))
          .map: _ =>
            Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show)
