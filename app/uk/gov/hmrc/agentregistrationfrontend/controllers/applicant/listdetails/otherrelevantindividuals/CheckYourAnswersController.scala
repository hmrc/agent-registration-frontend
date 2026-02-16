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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.otherrelevantindividuals

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsNotSoleTrader
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.AddOtherRelevantIndividualsForm
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.otherrelevantindividuals.CheckYourAnswersPage
import scala.concurrent.Future

@Singleton
class CheckYourAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: CheckYourAnswersPage,
  individualProvideDetailsService: IndividualProvideDetailsService,
  businessPartnerRecordService: BusinessPartnerRecordService
)
extends FrontendController(mcc, actions):

  private type DataWithLists = List[IndividualProvidedDetails] *: IsNotSoleTrader *: DataWithAuth

  private val baseAction: ActionBuilderWithData[DataWithLists] = actions
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
          case None => Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsController.show.url)
    .refine:
      implicit request =>
        val agentApplication: IsNotSoleTrader = request.get
        individualProvideDetailsService
          .findAllOtherRelevantIndividualsByApplicationId(
            agentApplication.agentApplicationId
          ).map[RequestWithData[
            DataWithLists
          ] | Result]:
            case Nil =>
              logger.warn(
                "Other relevant individuals specified in application, but no other relevant individuals found, redirecting to enter other relevant individual page"
              )
              Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.EnterOtherRelevantIndividualController.show.url)
            case list: List[IndividualProvidedDetails] => request.add[List[IndividualProvidedDetails]](list)

  def show: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        val agentApplication: IsNotSoleTrader = request.get[
          IsNotSoleTrader
        ]
        businessPartnerRecordService
          .getBusinessPartnerRecord(agentApplication.getUtr)
          .map: bprOpt =>
            Ok(view(
              existingList = request.get[List[IndividualProvidedDetails]],
              form = AddOtherRelevantIndividualsForm.form,
              entityName = bprOpt
                .map(_.getEntityName)
                .getOrThrowExpectedDataMissing(
                  "Business Partner Record is missing"
                )
            ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[Boolean](
        form = AddOtherRelevantIndividualsForm.form,
        resultToServeWhenFormHasErrors =
          implicit request =>
            formWithErrors => {
              val agentApplication: IsNotSoleTrader = request.get[
                IsNotSoleTrader
              ]
              businessPartnerRecordService
                .getBusinessPartnerRecord(agentApplication.getUtr)
                .map: bprOpt =>
                  view(
                    existingList = request.get[List[IndividualProvidedDetails]],
                    form = formWithErrors,
                    entityName = bprOpt
                      .map(_.getEntityName)
                      .getOrThrowExpectedDataMissing(
                        "Business Partner Record is missing"
                      )
                  )
            }
      )
      .async:
        implicit request =>
          val addOtherRelevantIndividuals: Boolean = request.get[Boolean]
          addOtherRelevantIndividuals match {
            case true => Future.successful(Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.EnterOtherRelevantIndividualController.show.url))
            case false => Future.successful(Redirect(AppRoutes.apply.listdetails.CheckYourAnswersController.show.url))
          }
      .redirectIfSaveForLater
