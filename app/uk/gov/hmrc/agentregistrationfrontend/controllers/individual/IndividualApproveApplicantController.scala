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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import com.softwaremill.quicklens.*
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualApproveApplicationForm
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo.toYesNo
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.IndividualApproveApplicationPage

import javax.inject.Inject

class IndividualApproveApplicantController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  view: IndividualApproveApplicationPage,
  individualProvideDetailsService: IndividualProvideDetailsService,
  businessPartnerRecordService: BusinessPartnerRecordService
)
extends FrontendController(mcc, actions):

  private def baseAction(linkId: LinkId): ActionBuilderWithData[DataWithIndividualProvidedDetails] = authorisedWithIndividualProvidedDetails(linkId)
    .ensure(
      _.get[IndividualProvidedDetails].individualSaUtr.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualSaUtrController.show(linkId).url)
    )
    .ensure(
      _.get[IndividualProvidedDetails].emailAddress.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualEmailAddressController.show(linkId).url)
    )

  def show(linkId: LinkId): Action[AnyContent] = baseAction(linkId)
    .refine:
      implicit request =>
        businessPartnerRecordService
          .getApplicationBusinessPartnerRecord(request.get[AgentApplication].getUtr)
          .map((bprOpt: Option[BusinessPartnerRecordResponse]) =>
            request.add(bprOpt.getOrThrowExpectedDataMissing(
              s"Business Partner Record for UTR ${request.get[AgentApplication].getUtr.value}"
            ))
          )
    .apply:
      implicit request =>
        val applicantName = request.get[AgentApplication].getApplicantContactDetails.applicantName
        val filledForm = IndividualApproveApplicationForm
          .form(applicantName.value)
          .fill:
            request.get[IndividualProvidedDetails]
              .hasApprovedApplication
              .map(_.toYesNo)
        Ok(
          view(
            form = filledForm,
            officerName = applicantName.value,
            entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
            linkId = linkId
          )
        )

  def submit(linkId: LinkId): Action[AnyContent] =
    baseAction(linkId)
      .refine:
        implicit request =>
          businessPartnerRecordService
            .getApplicationBusinessPartnerRecord(request.get[AgentApplication].getUtr)
            .map((bprOpt: Option[BusinessPartnerRecordResponse]) =>
              request.add(bprOpt.getOrThrowExpectedDataMissing(
                s"Business Partner Record for UTR ${request.get[AgentApplication].getUtr.value}"
              ))
            )
      .ensureValidFormAndRedirectIfSaveForLater[YesNo](
        implicit request =>
          val applicantName: ApplicantName = request.get[AgentApplication].getApplicantContactDetails.applicantName
          IndividualApproveApplicationForm.form(applicantName.value)
        ,
        implicit request =>
          val applicantName: ApplicantName = request.get[AgentApplication].getApplicantContactDetails.applicantName
          view(
            _,
            officerName = applicantName.value,
            entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
            linkId = linkId
          )
      )
      .async:
        implicit request =>
          val approved: Boolean = request.get[YesNo].toBoolean
          val updatedIndividualProvidedDetails: IndividualProvidedDetails = request.get[IndividualProvidedDetails]
            .modify(_.hasApprovedApplication)
            .setTo(Some(approved))

          individualProvideDetailsService
            .upsert(updatedIndividualProvidedDetails)
            .map: _ =>
              if approved then
                Redirect(AppRoutes.providedetails.IndividualHmrcStandardForAgentsController.show(linkId).url)
              else
                Redirect(AppRoutes.providedetails.IndividualConfirmStopController.show.url)
      .redirectIfSaveForLater
