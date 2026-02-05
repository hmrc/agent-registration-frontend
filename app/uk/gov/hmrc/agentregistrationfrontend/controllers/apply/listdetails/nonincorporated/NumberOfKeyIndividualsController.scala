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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.listdetails.nonincorporated

import com.softwaremill.quicklens.modify
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsAgentApplicationForDeclaringNumberOfKeyIndividuals
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishPartnership
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfRequiredKeyIndividuals
import uk.gov.hmrc.agentregistrationfrontend.action.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.NumberOfKeyIndividualsForm
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.listdetails.NumberOfKeyIndividualsPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NumberOfKeyIndividualsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  numberOfKeyIndividualsPage: NumberOfKeyIndividualsPage,
  agentApplicationService: AgentApplicationService,
  businessPartnerRecordService: BusinessPartnerRecordService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[
    IsAgentApplicationForDeclaringNumberOfKeyIndividuals *: DataWithAuth
  ] = actions
    .getApplicationInProgress
    .refine4:
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

  def show: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        val agentApplication: IsAgentApplicationForDeclaringNumberOfKeyIndividuals = request.get[IsAgentApplicationForDeclaringNumberOfKeyIndividuals]
        businessPartnerRecordService
          .getBusinessPartnerRecord(agentApplication.getUtr)
          .map: bprOpt =>
            Ok(numberOfKeyIndividualsPage(
              form = NumberOfKeyIndividualsForm.form
                .fill:
                  agentApplication.numberOfRequiredKeyIndividuals
              ,
              entityName = bprOpt
                .map(_.getEntityName)
                .getOrThrowExpectedDataMissing(
                  "Business Partner Record is missing"
                ),
              agentApplication = agentApplication
            ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater4[NumberOfRequiredKeyIndividuals](
        form = NumberOfKeyIndividualsForm.form,
        resultToServeWhenFormHasErrors =
          implicit request =>
            formWithErrors => {
              val agentApplication: IsAgentApplicationForDeclaringNumberOfKeyIndividuals = request.get[IsAgentApplicationForDeclaringNumberOfKeyIndividuals]
              businessPartnerRecordService
                .getBusinessPartnerRecord(agentApplication.getUtr)
                .map: bprOpt =>
                  numberOfKeyIndividualsPage(
                    form = formWithErrors,
                    entityName = bprOpt
                      .map(_.getEntityName)
                      .getOrThrowExpectedDataMissing(
                        "Business Partner Record is missing"
                      ),
                    agentApplication = agentApplication
                  )
            }
      )
      .async:
        implicit request =>
          val numberOfRequiredKeyIndividuals: NumberOfRequiredKeyIndividuals = request.get
          val updatedApplication: IsAgentApplicationForDeclaringNumberOfKeyIndividuals =
            request.get[IsAgentApplicationForDeclaringNumberOfKeyIndividuals] match
              case application: AgentApplicationScottishPartnership =>
                application
                  .modify(_.numberOfRequiredKeyIndividuals)
                  .setTo(Some(numberOfRequiredKeyIndividuals))
              case application: AgentApplicationGeneralPartnership =>
                application
                  .modify(_.numberOfRequiredKeyIndividuals)
                  .setTo(Some(numberOfRequiredKeyIndividuals))

          agentApplicationService
            .upsert(updatedApplication)
            .map: _ =>
              Redirect(AppRoutes.apply.listdetails.EnterKeyIndividualController.show.url)
      .redirectIfSaveForLater
