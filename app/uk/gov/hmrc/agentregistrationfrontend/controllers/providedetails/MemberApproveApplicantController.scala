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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails

import play.api.mvc.Action
import com.softwaremill.quicklens.modify
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.MemberProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.MemberApproveApplicationForm
import uk.gov.hmrc.agentregistrationfrontend.services.llp.MemberProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.memberconfirmation.MemberApproveApplicationPage
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo.toYesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo.toBoolean

import javax.inject.Inject

class MemberApproveApplicantController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  view: MemberApproveApplicationPage,
  memberProvideDetailsService: MemberProvideDetailsService,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[MemberProvideDetailsRequest, AnyContent] = actions.Member.getProvideDetailsInProgress
    .ensure(
      _.memberProvidedDetails.memberSaUtr.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.MemberSaUtrController.show.url)
    )

  def show: Action[AnyContent] = baseAction.async:
    implicit request =>
      agentApplicationService
        .find(request.memberProvidedDetails.agentApplicationId)
        .map:
          case Some(agentApplication) =>
            Ok(view(
              MemberApproveApplicationForm.form
                .fill:
                  request
                    .memberProvidedDetails
                    .hasApprovedApplication
                    // TODO PAV - remaping not the best idea, but we need to convert from YesNo to Boolean, any other way?
                    .map(_.toYesNo)
              ,
              request.memberProvidedDetails.getOfficerName,
              agentApplication.asLlpApplication.getBusinessDetails.companyProfile.companyName
            ))
          case None =>
            logger.info(s"Application for agent applicationId ${request.memberProvidedDetails.agentApplicationId} not found")
            Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)

  // TODO PAV - we do not have async version of ensureValidFormAndRedirectIfSaveForLater !?!?
  def submit: Action[AnyContent] =
    baseAction
      .async:
        implicit request =>
          MemberApproveApplicationForm.form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                agentApplicationService
                  .find(request.memberProvidedDetails.agentApplicationId)
                  .map:
                    case Some(agentApplication) =>
                      BadRequest(
                        view(
                          formWithErrors,
                          request.memberProvidedDetails.getOfficerName,
                          agentApplication.asLlpApplication.getBusinessDetails.companyProfile.companyName
                        )
                      )
                    case None => Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)
              ,
              validYesNo =>
                // TODO PAV - remaping not the best idea, but we need to convert from YesNo to Boolean, any other way?
                val approved = validYesNo.toBoolean

                val updatedApplication = request.memberProvidedDetails
                  .modify(_.hasApprovedApplication)
                  .setTo(Some(approved))

                memberProvideDetailsService
                  .upsert(updatedApplication)
                  .map: _ =>
                    if approved then
                      Redirect(AppRoutes.providedetails.MemberAgreeStandardController.show.url)
                    else
                      Redirect(AppRoutes.providedetails.MemberConfirmStopController.show.url)
            )
      .redirectIfSaveForLater
