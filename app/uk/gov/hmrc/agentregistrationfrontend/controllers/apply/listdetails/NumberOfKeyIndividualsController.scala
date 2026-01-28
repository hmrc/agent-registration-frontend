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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.listdetails

import com.softwaremill.quicklens.modify
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
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
  view: NumberOfKeyIndividualsPage,
  agentApplicationService: AgentApplicationService,
  businessPartnerRecordService: BusinessPartnerRecordService
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .async:
      implicit request =>
        businessPartnerRecordService
          .getBusinessPartnerRecord(request.agentApplication.getUtr)
          .map: bprOpt =>
            Ok(view(
              form = NumberOfKeyIndividualsForm.form
                .fill:
                  request.agentApplication.requiredKeyIndividuals
              ,
              entityName = bprOpt
                .map(_.getEntityName)
                .getOrThrowExpectedDataMissing(
                  "Business Partner Record is missing"
                )
            ))

  def submit: Action[AnyContent] =
    actions
      .Applicant
      .getApplicationInProgress
      .ensureValidFormAndRedirectIfSaveForLaterAsync[NumberOfRequiredKeyIndividuals](
        form = NumberOfKeyIndividualsForm.form,
        viewToServeWhenFormHasErrors =
          implicit request =>
            formWithErrors =>
              businessPartnerRecordService
                .getBusinessPartnerRecord(request.agentApplication.getUtr)
                .map: bprOpt =>
                  view(
                    form = formWithErrors,
                    entityName = bprOpt
                      .map(_.getEntityName)
                      .getOrThrowExpectedDataMissing(
                        "Business Partner Record is missing"
                      )
                  )
      )
      .async:
        implicit request: (AgentApplicationRequest[AnyContent] & FormValue[NumberOfRequiredKeyIndividuals]) =>
          val requiredKeyIndividuals: NumberOfRequiredKeyIndividuals = request.formValue
          logger.warn(s"Number of key individuals selected: $requiredKeyIndividuals")
          val updatedApplication: AgentApplication = request
            .agentApplication
            .modify(_.requiredKeyIndividuals)
            .setTo(Some(requiredKeyIndividuals)) // TODO: work out how to preserve/copy any existing lists when changing number/type

          agentApplicationService
            .upsert(updatedApplication)
            .map: _ =>
              Redirect(AppRoutes.apply.listdetails.EnterKeyIndividualController.show.url)
      .redirectIfSaveForLater
