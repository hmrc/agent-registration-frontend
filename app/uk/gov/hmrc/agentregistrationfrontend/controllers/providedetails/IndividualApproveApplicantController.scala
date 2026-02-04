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
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.action.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.action.individual.llp.IndividualProvideDetailsWithApplicationRequest

import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualApproveApplicationForm
import uk.gov.hmrc.agentregistrationfrontend.services.llp.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.individualconfirmation.IndividualApproveApplicationPage
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo.toYesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo.toBoolean

import scala.concurrent.Future
import javax.inject.Inject

class IndividualApproveApplicantController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  view: IndividualApproveApplicationPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[IndividualProvideDetailsWithApplicationRequest, AnyContent] = actions.getProvideDetailsWithApplicationInProgress
    .ensure(
      _.individualProvidedDetails.individualSaUtr.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualSaUtrController.show.url)
    )
    .ensure(
      _.individualProvidedDetails.emailAddress.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualEmailAddressController.show.url)
    )

  def show: Action[AnyContent] = baseAction.async:
    implicit request =>
      val applicantName = request.agentApplication.asLlpApplication.getApplicantContactDetails.applicantName
      val companyName = request.agentApplication.asLlpApplication.getBusinessDetails.companyProfile.companyName
      val filledForm = IndividualApproveApplicationForm
        .form(applicantName.value)
        .fill:
          request
            .individualProvidedDetails
            .hasApprovedApplication
            .map(_.toYesNo)

      Future.successful(
        Ok(
          view(
            form = filledForm,
            officerName = applicantName.value,
            companyName = companyName
          )
        )
      )

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[YesNo](
        implicit r =>
          val applicantName = r.agentApplication.asLlpApplication.getApplicantContactDetails.applicantName
          IndividualApproveApplicationForm.form(applicantName.value)
        ,
        implicit r =>
          val applicantName = r.agentApplication.asLlpApplication.getApplicantContactDetails.applicantName
          val companyName = r.agentApplication.asLlpApplication.getBusinessDetails.companyProfile.companyName
          view(
            _,
            officerName = applicantName.value,
            companyName = companyName
          )
      )
      .async:
        implicit r: (IndividualProvideDetailsWithApplicationRequest[AnyContent] & FormValue[YesNo]) =>
          val approved: Boolean = r.formValue.toBoolean

          val updatedApplication: IndividualProvidedDetailsToBeDeleted = r.individualProvidedDetails
            .modify(_.hasApprovedApplication)
            .setTo(Some(approved))

          individualProvideDetailsService
            .upsert(updatedApplication)
            .map: _ =>
              if approved then
                Redirect(AppRoutes.providedetails.IndividualHmrcStandardForAgentsController.show.url)
              else
                Redirect(AppRoutes.providedetails.IndividualConfirmStopController.show.url)
      .redirectIfSaveForLater
