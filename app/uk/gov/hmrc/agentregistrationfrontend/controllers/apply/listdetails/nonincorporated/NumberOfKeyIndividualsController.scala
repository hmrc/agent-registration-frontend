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
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishPartnership
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfRequiredKeyIndividuals
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.NumberOfKeyIndividualsForm
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.listdetails.NumberOfKeyIndividualsPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NumberOfKeyIndividualsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  numberOfKeyIndividualsPage: NumberOfKeyIndividualsPage,
  agentApplicationService: AgentApplicationService,
  businessPartnerRecordService: BusinessPartnerRecordService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      _.agentApplication match
        case _: AgentApplication.IsNotSoleTrader => true
        case _: AgentApplication.IsSoleTrader => false
      ,
      implicit request =>
        logger.warn("Sole traders cannot specify number of key individuals, redirecting to task list for the correct links")
        Redirect(AppRoutes.apply.TaskListController.show.url)
    )
    .ensure(
      _.agentApplication match
        case _: AgentApplication.IsNotIncorporated => true
        case _: AgentApplication.IsIncorporated => false
      ,
      implicit request =>
        logger.warn(
          "Incorporated businesses should have the number of key individuals determined by Companies House results, redirecting to task list for the correct links"
        )
        Redirect(AppRoutes.apply.TaskListController.show.url)
    )

  def show: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        businessPartnerRecordService
          .getBusinessPartnerRecord(request.agentApplication.getUtr)
          .map: bprOpt =>
            Ok(numberOfKeyIndividualsPage(
              form = NumberOfKeyIndividualsForm.form
                .fill:
                  request.agentApplication.numberOfRequiredKeyIndividuals
              ,
              entityName = bprOpt
                .map(_.getEntityName)
                .getOrThrowExpectedDataMissing(
                  "Business Partner Record is missing"
                ),
              agentApplication = request.agentApplication
            ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLaterAsync[NumberOfRequiredKeyIndividuals](
        form = NumberOfKeyIndividualsForm.form,
        viewToServeWhenFormHasErrors =
          implicit request =>
            formWithErrors =>
              businessPartnerRecordService
                .getBusinessPartnerRecord(request.agentApplication.getUtr)
                .map: bprOpt =>
                  numberOfKeyIndividualsPage(
                    form = formWithErrors,
                    entityName = bprOpt
                      .map(_.getEntityName)
                      .getOrThrowExpectedDataMissing(
                        "Business Partner Record is missing"
                      ),
                    agentApplication = request.agentApplication
                  )
      )
      .async:
        implicit request: (AgentApplicationRequest[AnyContent] & FormValue[NumberOfRequiredKeyIndividuals]) =>
          request.agentApplication match
            case _: AgentApplication.IsSoleTrader =>
              // this should never happen because of the baseAction guard
              throw new IllegalStateException("Sole traders cannot specify number of key individuals")
            case _: AgentApplication.IsIncorporated =>
              // this should never happen because of the baseAction guard
              throw new IllegalStateException("Incorporated businesses should have the number of key individuals determined by Companies House results")
            case application: (AgentApplication.IsNotSoleTrader & AgentApplication.IsNotIncorporated) =>
              val numberOfRequiredKeyIndividuals: NumberOfRequiredKeyIndividuals = request.formValue
              val updatedApplication =
                application match
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
