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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistration.shared.businessdetails.BusinessDetailsSoleTrader
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.soletrader.AskOwnerToProveIdentityPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AskOwnerToProveIdentityController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: AskOwnerToProveIdentityPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private type DataWithSoleTrader = IndividualProvidedDetails *: List[IndividualProvidedDetails] *: AgentApplicationSoleTrader *: DataWithAuth

  private val baseAction: ActionBuilderWithData[DataWithSoleTrader] = actions
    .getApplicationInProgress
    .refine(implicit request =>
      request.get[AgentApplication] match
        case agentApplication: AgentApplicationSoleTrader => request.replace[AgentApplication, AgentApplicationSoleTrader](agentApplication)
        case _ =>
          logger.warn(s"Application is not a sole trader application, applicationId:[${request.get[AgentApplication].agentApplicationId}], redirecting to task list for overview")
          Redirect(AppRoutes.apply.TaskListController.show)
    )
    .ensure(
      _.get[AgentApplicationSoleTrader].getUserRole === UserRole.Authorised,
      implicit request =>
        logger.warn(s"${request.get[AgentApplicationSoleTrader].getUserRole} is the wrong role, we require ${UserRole.Authorised.toString} to access the link for the sole trader owner, applicationId:[${request.get[AgentApplicationSoleTrader].agentApplicationId}], redirecting to task list for overview")
        Redirect(AppRoutes.apply.TaskListController.show)
    )
    .refine(implicit request =>
      val agentApplication: AgentApplicationSoleTrader = request.get
      individualProvideDetailsService.findAllByApplicationId(agentApplication.agentApplicationId).map: individualsList =>
        request.add[List[IndividualProvidedDetails]](individualsList)
    )
    .refine(implicit request =>
      val existingList: List[IndividualProvidedDetails] = request.get
      val agentApplication: AgentApplicationSoleTrader = request.get
      existingList match
        case Nil =>
          // there is no existing individual record for this application,
          // so we need to automate the creation of one based on the application data as this is a sole trader application
          val soleTraderDetails: BusinessDetailsSoleTrader = agentApplication.getBusinessDetails
          val baseRecord = individualProvideDetailsService.create(
            agentApplicationId = agentApplication.agentApplicationId,
            individualName = IndividualName(s"${soleTraderDetails.fullName.firstName} ${soleTraderDetails.fullName.lastName}"),
            isPersonOfControl = true // this individual record is for the sole trader owner not the applicant
          )
          individualProvideDetailsService
            .upsertForApplication(baseRecord)
            .map: _ =>
              request.add[IndividualProvidedDetails](baseRecord)
        case soleTrader :: Nil => request.add[IndividualProvidedDetails](soleTrader) // we have already visited this page and the record has been created
        case _ =>
          logger.warn(s"Unexpected multiple provided details records for sole trader application, applicationId:[${agentApplication.agentApplicationId}], cannot recover from this state so exiting")
          Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        agentApplication = request.get[AgentApplicationSoleTrader]
      ))

  def submit: Action[AnyContent] = baseAction.async:
    implicit request =>
      val individualProvidedDetails: IndividualProvidedDetails = request.get
      val updatedRecord = individualProvidedDetails.copy(
        providedDetailsState = ProvidedDetailsState.AccessConfirmed
      )
      individualProvideDetailsService
        .upsertForApplication(updatedRecord)
        .map: _ =>
          Redirect(AppRoutes.apply.TaskListController.show.url)
