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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.nonincorporated

import com.softwaremill.quicklens.modify
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsUnincorporatedPartnership
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.listdetails.nonincorporated.NumberOfPartnersResponsibleForTaxForm
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.nonincorporated.NumberOfPartnersResponsibleForTaxPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NumberOfPartnersResponsibleForTaxController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  numberOfPartnersResponsibleForTaxPage: NumberOfPartnersResponsibleForTaxPage,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  private type DataWithSixOrMore = SixOrMore *: BusinessPartnerRecordResponse *: IsUnincorporatedPartnership *: DataWithAuth

  private val baseAction: ActionBuilderWithData[
    DataWithSixOrMore
  ] = actions
    .getApplicationInProgress
    .getBusinessPartnerRecord
    .refine:
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
          case aa: IsUnincorporatedPartnership => request.replace[AgentApplication, IsUnincorporatedPartnership](aa)
    .refine:
      implicit request =>
        val agentApplication: IsUnincorporatedPartnership = request.get
        agentApplication.numberOfIndividuals match
          case Some(a @ SixOrMore(_)) => request.add[SixOrMore](a)
          case _ =>
            logger.warn(
              "Number of key individuals responsible for tax matters is only applicable to partnerships with six or more key individuals, redirecting to number of partners page to select the number type"
            )
            Redirect(AppRoutes.apply.listdetails.nonincorporated.NumberOfPartnersController.show.url)

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val sixOrMore: SixOrMore = request.get
      Ok(numberOfPartnersResponsibleForTaxPage(
        form = NumberOfPartnersResponsibleForTaxForm.form
          .fill:
            sixOrMore.numberOfKeyIndividualsResponsibleForTaxMatters
        ,
        entityName = request.get[BusinessPartnerRecordResponse].getEntityName
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[Int](
        form = NumberOfPartnersResponsibleForTaxForm.form,
        resultToServeWhenFormHasErrors =
          implicit request =>
            formWithErrors =>
              BadRequest(numberOfPartnersResponsibleForTaxPage(
                form = formWithErrors,
                entityName = request.get[BusinessPartnerRecordResponse].getEntityName
              ))
      )
      .async:
        implicit request =>
          val agentApplication: IsUnincorporatedPartnership = request.get
          val numberOfPartners: Int = request.get
          val sixOrMore: SixOrMore = request.get
          val updatedSixOrMore: SixOrMore = sixOrMore.copy(
            numberOfKeyIndividualsResponsibleForTaxMatters = Some(numberOfPartners)
          )
          val updatedApplication: IsUnincorporatedPartnership =
            agentApplication match
              case application: AgentApplicationScottishPartnership =>
                application
                  .modify(_.numberOfIndividuals)
                  .setTo(Some(updatedSixOrMore))
              case application: AgentApplicationGeneralPartnership =>
                application
                  .modify(_.numberOfIndividuals)
                  .setTo(Some(updatedSixOrMore))

          agentApplicationService
            .upsert(updatedApplication)
            .map: _ =>
              Redirect(AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater
