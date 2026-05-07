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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.nonincorporated

import com.softwaremill.quicklens.modify
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsAgentApplicationForDeclaringNumberOfKeyIndividuals
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfRequiredKeyIndividuals
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.NumberOfKeyIndividualsForm
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.nonincorporated.NumberOfKeyIndividualsPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NumberOfKeyIndividualsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  numberOfKeyIndividualsPage: NumberOfKeyIndividualsPage,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  type DataWithValidApplicationAndBpr = BusinessPartnerRecordResponse *: IsAgentApplicationForDeclaringNumberOfKeyIndividuals *: DataWithAuth

  private val baseAction: ActionBuilderWithData[
    DataWithValidApplicationAndBpr
  ] = actions
    .getApplicationInProgress
    .getBusinessPartnerRecord
    .refine:
      implicit request =>
        request.agentApplication match
          case _: AgentApplication.IsIncorporated =>
            logger.warn(
              "Incorporated businesses should have the number of key individuals determined by Companies House results, redirecting to task list for the correct links"
            )
            Redirect(AppRoutes.apply.TaskListController.show.url)
          case _: AgentApplication.IsSoleTrader =>
            logger.warn("Sole traders cannot specify number of key individuals, redirecting to task list for the correct links")
            Redirect(AppRoutes.apply.TaskListController.show.url)
          case aa: IsAgentApplicationForDeclaringNumberOfKeyIndividuals =>
            request.replace[AgentApplication, IsAgentApplicationForDeclaringNumberOfKeyIndividuals](aa)

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val agentApplication: IsAgentApplicationForDeclaringNumberOfKeyIndividuals = request.get
      Ok(numberOfKeyIndividualsPage(
        form = NumberOfKeyIndividualsForm.form
          .fill:
            agentApplication.getNumberOfRequiredKeyIndividuals
        ,
        entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
        agentApplication = agentApplication
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[NumberOfRequiredKeyIndividuals](
        form = NumberOfKeyIndividualsForm.form,
        resultToServeWhenFormHasErrors =
          implicit request =>
            formWithErrors =>
              val agentApplication: IsAgentApplicationForDeclaringNumberOfKeyIndividuals = request.get[IsAgentApplicationForDeclaringNumberOfKeyIndividuals]
              numberOfKeyIndividualsPage(
                form = formWithErrors,
                entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
                agentApplication = agentApplication
              )
      )
      .async:
        implicit request =>
          val agentApplication: IsAgentApplicationForDeclaringNumberOfKeyIndividuals = request.get
          val numberOfRequiredKeyIndividuals: NumberOfRequiredKeyIndividuals = request.get[NumberOfRequiredKeyIndividuals]

          /** When zero key individuals (partners) has been specified then user must supply at least one other relevant individual, we infer Some(true) for
            * hasOtherRelevantIndividuals in this case to ensure they are taken to the correct page to add other relevant individuals. We must also be careful
            * to preserve any existing value if not zero key individuals as we don't want to override a user's answer
            */
          val requiresOtherRelevantIndividuals: Option[Boolean] =
            if numberOfRequiredKeyIndividuals.numberOfIndividuals === 0
            then Some(true)
            else if agentApplication.hasOtherRelevantIndividuals.isDefined
            then agentApplication.hasOtherRelevantIndividuals
            else None
          val updatedApplication: IsAgentApplicationForDeclaringNumberOfKeyIndividuals =
            agentApplication match
              case application: AgentApplicationScottishPartnership =>
                application
                  .modify(_.numberOfIndividuals)
                  .setTo(Some(numberOfRequiredKeyIndividuals))
                  .modify(_.hasOtherRelevantIndividuals)
                  .setTo(requiresOtherRelevantIndividuals)
              case application: AgentApplicationGeneralPartnership =>
                application
                  .modify(_.numberOfIndividuals)
                  .setTo(Some(numberOfRequiredKeyIndividuals))
                  .modify(_.hasOtherRelevantIndividuals)
                  .setTo(requiresOtherRelevantIndividuals)

          agentApplicationService
            .upsert(updatedApplication)
            .map: _ =>
              Redirect(AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater
