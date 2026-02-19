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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.soletrader

import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsSoleTrader
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.soletrader.ProveIdentityPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProveIdentityController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: ProveIdentityPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private type DataWithSoleTrader = IndividualProvidedDetails *: List[IndividualProvidedDetails] *: DataWithApplication

  private val baseAction: ActionBuilderWithData[DataWithSoleTrader] = actions
    .getApplicationInProgress
    .refine(implicit request =>
      val agentApplication: AgentApplication = request.get
      individualProvideDetailsService.findAllByApplicationId(agentApplication.agentApplicationId).map: individualsList =>
        request.add[List[IndividualProvidedDetails]](individualsList)
    )
    .refine(implicit request =>
      val existingList: List[IndividualProvidedDetails] = request.get
      val agentApplication: AgentApplication = request.get
      existingList match
        case Nil =>
          agentApplication match
            case agentApplication: AgentApplicationSoleTrader =>
              val soleTraderDetails: BusinessDetailsSoleTrader = agentApplication.getBusinessDetails
              val applicantContactDetails: ApplicantContactDetails = agentApplication.getApplicantContactDetails
              val newRecord = individualProvideDetailsService.create(
                agentApplicationId = agentApplication.agentApplicationId,
                individualName = IndividualName(s"${soleTraderDetails.fullName.firstName} ${soleTraderDetails.fullName.lastName}"),
                isPersonOfControl = agentApplication.getUserRole === UserRole.Owner
              )
                .modify(_.providedDetailsState)
                .setTo(ProvidedDetailsState.AccessConfirmed)
                .modify(_.telephoneNumber)
                .setTo(applicantContactDetails.telephoneNumber)
                .modify(_.emailAddress)
                .setTo(Some(IndividualVerifiedEmailAddress(
                  emailAddress = applicantContactDetails.getVerifiedEmail,
                  isVerified = true
                )))
                .modify(_.hasApprovedApplication)
                .setTo(Some(true)) // Sole trader owners applicants are always approved as they are the same person
                .modify(_.hmrcStandardForAgentsAgreed)
                .setTo(agentApplication.hmrcStandardForAgentsAgreed)
              individualProvideDetailsService
                .upsertForApplication(newRecord)
                .map: _ =>
                  request.add[IndividualProvidedDetails](newRecord)
            case _: AgentApplication.IsNotSoleTrader =>
              logger.warn(s"Unexpected application type for sole trader details, applicationId:[${agentApplication.agentApplicationId}], redirect to task list for overview")
              Redirect(AppRoutes.apply.TaskListController.show)
        case soleTrader :: Nil => request.add[IndividualProvidedDetails](soleTrader)
        case _ =>
          logger.warn(s"Unexpected multiple provided details records for sole trader application, applicationId:[${agentApplication.agentApplicationId}], redirect to task list for overview")
          Redirect(AppRoutes.apply.TaskListController.show)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val individualProvidedDetails: IndividualProvidedDetails = request.get
      Ok(view(
        agentApplication = request.get[AgentApplication],
        hasProvedIdentity = individualProvidedDetails.hasFinished
      ))
