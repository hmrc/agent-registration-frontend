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
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfPartners
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.listdetails.nonincorporated.NumberOfPartnersForm
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.nonincorporated.NumberOfPartnersPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class NumberOfPartnersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  numberOfPartnersPage: NumberOfPartnersPage,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  type DataWithValidApplicationAndBpr = BusinessPartnerRecordResponse *: IsUnincorporatedPartnership *: DataWithAuth

  private val baseAction: ActionBuilderWithData[
    DataWithValidApplicationAndBpr
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

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val agentApplication: IsUnincorporatedPartnership = request.get
      Ok(numberOfPartnersPage(
        form = NumberOfPartnersForm.form
          .fill:
            agentApplication.getNumberOfPartners
        ,
        entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
        agentApplication = agentApplication
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[NumberOfPartners](
        form = NumberOfPartnersForm.form,
        resultToServeWhenFormHasErrors =
          implicit request =>
            formWithErrors =>
              val agentApplication: IsUnincorporatedPartnership = request.get[IsUnincorporatedPartnership]
              numberOfPartnersPage(
                form = formWithErrors,
                entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
                agentApplication = agentApplication
              )
      )
      .async:
        implicit request =>
          val agentApplication: IsUnincorporatedPartnership = request.get
          val numberOfPartners: NumberOfPartners = request.get[NumberOfPartners]

          (numberOfPartners, agentApplication.numberOfIndividuals) match
            case (_: SixOrMore, Some(SixOrMore(_))) =>
              /** When [[SixOrMore]] is submitted as the choice it will not include the inner value for the number of partners. We need to check if the previous
                * value was also [[SixOrMore]] and, if so, not overwrite the existing inner value with the default None. We redirect to where the inner
                * [[SixOrMore.numberOfKeyIndividualsResponsibleForTaxMatters]] can be edited as the CYA link only goes to this page and there is no other link
                * to edit this.
                */
              Future.successful(Redirect(AppRoutes.apply.listdetails.nonincorporated.NumberOfPartnersResponsibleForTaxController.show.url))
            case (FiveOrLess(a), Some(FiveOrLess(b))) if a === b =>
              /** When [[FiveOrLess]] is submitted as the choice it must include the value for [[FiveOrLess.numberOfKeyIndividuals]]. We need to check if the
                * previous value was identical and, if so, not overwrite the existing value.
                */
              Future.successful(Redirect(AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show.url))
            case _ =>
              val updatedApplication: IsUnincorporatedPartnership =
                agentApplication match
                  case application: AgentApplicationScottishPartnership =>
                    application
                      .modify(_.numberOfIndividuals)
                      .setTo(Some(numberOfPartners))
                  case application: AgentApplicationGeneralPartnership =>
                    application
                      .modify(_.numberOfIndividuals)
                      .setTo(Some(numberOfPartners))

              agentApplicationService
                .upsert(updatedApplication)
                .map: _ =>
                  Redirect(AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater
