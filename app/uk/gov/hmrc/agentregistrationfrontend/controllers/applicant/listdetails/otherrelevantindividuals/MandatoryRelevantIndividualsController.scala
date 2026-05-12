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
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsNotSoleTrader
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.OtherRelevantIndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.otherrelevantindividuals.MandatoryRelevantIndividualsPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class MandatoryRelevantIndividualsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  mandatoryRelevantIndividualsPage: MandatoryRelevantIndividualsPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[List[IndividualProvidedDetails] *: BusinessPartnerRecordResponse *: IsNotSoleTrader *: DataWithAuth] = actions
    .getApplicationInProgress
    .getBusinessPartnerRecord
    .refine:
      implicit request =>
        request.agentApplication match
          case _: AgentApplication.IsSoleTrader =>
            logger.warn("Sole traders cannot specify other relevant individuals, redirecting to task list for the correct links")
            Redirect(AppRoutes.apply.TaskListController.show.url)
          case aa: IsNotSoleTrader => request.replace[AgentApplication, IsNotSoleTrader](aa)
    .refine:
      implicit request =>
        val agentApplication: IsNotSoleTrader = request.get[IsNotSoleTrader]
        individualProvideDetailsService
          .findAllOtherRelevantIndividualsByApplicationId(agentApplication.agentApplicationId)
          .map: individualsList =>
            request.add[List[IndividualProvidedDetails]](individualsList)

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(mandatoryRelevantIndividualsPage(
        form = OtherRelevantIndividualNameForm.form,
        entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
        businessTypeKey = businessTypeKey(request.get[IsNotSoleTrader])
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[IndividualName](
        form = OtherRelevantIndividualNameForm.form,
        resultToServeWhenFormHasErrors =
          implicit request =>
            (formWithErrors: Form[IndividualName]) =>
              Future.successful(BadRequest(mandatoryRelevantIndividualsPage(
                form = formWithErrors,
                entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
                businessTypeKey = businessTypeKey(request.get[IsNotSoleTrader])
              )))
      )
      .async:
        implicit request =>
          val individualName: IndividualName = request.get
          for
            individualProvidedDetails: IndividualProvidedDetails <- individualProvideDetailsService.create(
              individualName = individualName,
              isPersonOfControl = false,
              agentApplicationId = request.get[IsNotSoleTrader].agentApplicationId
            )
            _ <- individualProvideDetailsService.upsertForApplication(individualProvidedDetails)
          yield Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show)
      .redirectIfSaveForLater

  private def businessTypeKey(agentApplication: IsNotSoleTrader): String =
    agentApplication match
      case _: AgentApplicationLlp => "LimitedLiabilityPartnership"
      case _: AgentApplicationLimitedCompany => "LimitedCompany"
      case _ => "Partnership"
