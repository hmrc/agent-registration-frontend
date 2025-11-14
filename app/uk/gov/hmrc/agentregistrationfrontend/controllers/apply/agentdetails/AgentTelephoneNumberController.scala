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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.agentdetails

import com.softwaremill.quicklens.*
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentDetails
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentTelephoneNumberForm
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.SubmissionHelper
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.agentdetails.AgentTelephoneNumberPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.util.chaining.scalaUtilChainingOps

@Singleton
class AgentTelephoneNumberController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: AgentTelephoneNumberPage,
  agentApplicationService: AgentApplicationService,
  businessPartnerRecordService: BusinessPartnerRecordService
)(using ec: ExecutionContext)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions.getApplicationInProgress
    .ensure(
      _
        .agentApplication
        .asLlpApplication
        .agentDetails
        .isDefined,
      implicit request =>
        logger.warn("Because we don't have a business name selected we are redirecting to the business name page")
        Redirect(routes.AgentBusinessNameController.show)
    )

  def show: Action[AnyContent] = baseAction.async:
    implicit request =>
      businessPartnerRecordService
        .getBusinessPartnerRecord(
          Utr(request.agentApplication.asLlpApplication.getBusinessDetails.saUtr.value)
        ).map: bprOpt =>
          Ok(view(
            form = AgentTelephoneNumberForm.form.fill:
              request
                .agentApplication
                .asLlpApplication
                .agentDetails.flatMap(_.telephoneNumber)
            ,
            bprTelephoneNumber = bprOpt.flatMap(_.primaryPhoneNumber)
          ))

  def submit: Action[AnyContent] =
    baseAction
      .async:
        implicit request: AgentApplicationRequest[AnyContent] =>
          AgentTelephoneNumberForm.form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                businessPartnerRecordService
                  .getBusinessPartnerRecord(
                    Utr(request.agentApplication.asLlpApplication.getBusinessDetails.saUtr.value)
                  ).map: bprOpt =>
                    BadRequest(
                      view(
                        form = formWithErrors,
                        bprTelephoneNumber = bprOpt.flatMap(_.primaryPhoneNumber)
                      )
                    ).pipe(SubmissionHelper.redirectIfSaveForLater(request, _)),
              telephoneNumberFromForm =>
                val updatedApplication: AgentApplication = request
                  .agentApplication
                  .asLlpApplication
                  .modify(_.agentDetails.each.telephoneNumber)
                  .setTo(Some(telephoneNumberFromForm))
                agentApplicationService
                  .upsert(updatedApplication)
                  .map: _ =>
                    Redirect(routes.CheckYourAnswersController.show.url)
            )
      .redirectIfSaveForLater
