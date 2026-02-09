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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.nonincorporated.CheckYourAnswersPage

@Singleton
class CheckYourAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: CheckYourAnswersPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[
    List[IndividualProvidedDetails] *: DataWithApplication
  ] = actions
    .getApplicationInProgress
    .refine(implicit request =>
      val agentApplication: AgentApplication = request.get
      individualProvideDetailsService.findAllByApplicationId(agentApplication.agentApplicationId).map: individualsList =>
        request.add[List[IndividualProvidedDetails]](individualsList)
    )
    .ensure(
      _.get[List[IndividualProvidedDetails]].nonEmpty,
      resultWhenConditionNotMet =
        implicit request =>
          request.get[AgentApplication] match
            case _: AgentApplicationSoleTrader =>
              logger.warn("Sole traders should not be able to access this page, redirecting to generic exit page.")
              Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)
            case _: AgentApplication.IsNotIncorporated =>
              logger.warn("Because we have no individuals added, redirecting to where this can be managed")
              Redirect(AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show)
            case _: AgentApplication.IsIncorporated =>
              logger.warn("Incorporated businesses have not been developed yet, redirecting to generic exit page.")
              Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val existingList: List[IndividualProvidedDetails] = request.get
      request.get[AgentApplication] match
        case _: AgentApplicationSoleTrader => Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)
        case b: AgentApplication.IsNotIncorporated =>
          if b.listComplete(existingList.size)
          then Ok(view(b, existingList))
          else Redirect(AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show.url)
        case _ =>
          logger.warn("Incorporated businesses have not been developed yet")
          Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)

  extension (agentApplication: AgentApplication.IsNotIncorporated)
    def listComplete(size: Int): Boolean =
      agentApplication.numberOfRequiredKeyIndividuals match
        case Some(FiveOrLess(a: Int)) => size === a
        case Some(a @ SixOrMore(_)) => size === (a.numberOfKeyIndividualsResponsibleForTaxMatters + a.requiredPadding)
        case _ => false
