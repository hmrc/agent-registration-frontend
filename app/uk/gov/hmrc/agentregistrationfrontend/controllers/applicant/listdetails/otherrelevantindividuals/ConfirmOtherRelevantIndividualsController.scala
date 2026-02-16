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
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsNotSoleTrader
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.ConfirmOtherRelevantIndividualsForm
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.otherrelevantindividuals.ConfirmOtherRelevantIndividualsPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfirmOtherRelevantIndividualsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  confirmOtherRelevantIndividualsPage: ConfirmOtherRelevantIndividualsPage,
  agentApplicationService: AgentApplicationService,
  businessPartnerRecordService: BusinessPartnerRecordService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[IsNotSoleTrader *: DataWithAuth] = actions
    .getApplicationInProgress
    .refine:
      implicit request =>
        request.agentApplication match
          case _: AgentApplication.IsSoleTrader =>
            logger.warn("Sole traders cannot specify other relevant individuals, redirecting to task list for the correct links")
            Redirect(AppRoutes.apply.TaskListController.show.url)
          case aa: IsNotSoleTrader => request.replace[AgentApplication, IsNotSoleTrader](aa)

  def show: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        val agentApplication: IsNotSoleTrader = request.get[
          IsNotSoleTrader
        ]

        businessPartnerRecordService
          .getBusinessPartnerRecord(agentApplication.getUtr)
          .map: bprOpt =>
            Ok(confirmOtherRelevantIndividualsPage(
              form =
                agentApplication
                  .hasOtherRelevantIndividuals
                  .fold(ConfirmOtherRelevantIndividualsForm.form)(ConfirmOtherRelevantIndividualsForm.form.fill),
              entityName = bprOpt
                .map(_.getEntityName)
                .getOrThrowExpectedDataMissing(
                  "Business Partner Record is missing"
                ),
              agentApplication = agentApplication
            ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[Boolean](
        form = ConfirmOtherRelevantIndividualsForm.form,
        resultToServeWhenFormHasErrors =
          implicit request =>
            formWithErrors => {
              val agentApplication: IsNotSoleTrader = request.get[IsNotSoleTrader]
              businessPartnerRecordService
                .getBusinessPartnerRecord(agentApplication.getUtr)
                .map: bprOpt =>
                  confirmOtherRelevantIndividualsPage(
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
          val hasOtherRelevantIndividuals: Boolean = request.get[Boolean]

          val updatedApplication: IsNotSoleTrader = {
            request.get[IsNotSoleTrader] match
              case application: AgentApplicationLimitedCompany =>
                application
                  .modify(_.hasOtherRelevantIndividuals)
                  .setTo(Some(hasOtherRelevantIndividuals))
              case application: AgentApplicationLimitedPartnership =>
                application
                  .modify(_.hasOtherRelevantIndividuals)
                  .setTo(Some(hasOtherRelevantIndividuals))
              case application: AgentApplicationLlp =>
                application
                  .modify(_.hasOtherRelevantIndividuals)
                  .setTo(Some(hasOtherRelevantIndividuals))
              case application: AgentApplicationScottishLimitedPartnership =>
                application
                  .modify(_.hasOtherRelevantIndividuals)
                  .setTo(Some(hasOtherRelevantIndividuals))
              case application: AgentApplicationGeneralPartnership =>
                application
                  .modify(_.hasOtherRelevantIndividuals)
                  .setTo(Some(hasOtherRelevantIndividuals))
              case application: AgentApplicationScottishPartnership =>
                application
                  .modify(_.hasOtherRelevantIndividuals)
                  .setTo(Some(hasOtherRelevantIndividuals))
          }

          agentApplicationService
            .upsert(updatedApplication)
            .map: _ =>
              Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater
