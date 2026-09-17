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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.incorporated

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsIncorporated
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.CheckYourAnswersPage

@Singleton
class CheckYourAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: CheckYourAnswersPage
)
extends FrontendController(mcc, actions):

  private type DataWithLists = List[IndividualProvidedDetails] *: SixOrMoreOfficers *: IsIncorporated *: DataWithAuth

  private val baseAction: ActionBuilderWithData[DataWithLists] = actions
    .getIncorporatedApplication
    .getSixOrMoreOfficers(redirectWhenFiveOrLess = AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show)
    .getCompaniesHouseKeyIndividuals
    .ensure(
      condition = request => request.get[List[IndividualProvidedDetails]].nonEmpty || request.get[SixOrMoreOfficers].totalListSize === 0,
      resultWhenConditionNotMet =
        implicit request =>
          logger.debug(
            "Number of required companies house officers specified in application, but no officers found, redirecting to number of enter companies house officers page"
          )
          Redirect(AppRoutes.apply.listdetails.incoporated.EnterCompaniesHouseOfficerController.show.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val agentApplication: IsIncorporated = request.get[IsIncorporated]
      Ok(view(
        sixOrMoreOfficers = request.get[SixOrMoreOfficers],
        existingList = request.get[List[IndividualProvidedDetails]],
        agentApplication = agentApplication
      ))
