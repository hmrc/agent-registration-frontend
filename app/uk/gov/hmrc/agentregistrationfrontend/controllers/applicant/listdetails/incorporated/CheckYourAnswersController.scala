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
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsIncorporated
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.CheckYourAnswersPage

@Singleton
class CheckYourAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: CheckYourAnswersPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private type DataWithLists = List[IndividualProvidedDetails] *: SixOrMoreOfficers *: IsIncorporated *: DataWithAuth

  private val baseAction: ActionBuilderWithData[DataWithLists] = actions
    .getApplicationInProgress
    .refine:
      implicit request =>
        request.agentApplication match
          case _: AgentApplication.IsNotIncorporated =>
            logger.warn(
              "NotIncorporated businesses do not have the number of key individuals determined by Companies House results, redirecting to task list for the correct links"
            )
            Redirect(AppRoutes.apply.TaskListController.show.url)
          case aa: IsIncorporated => request.replace[AgentApplication, IsIncorporated](aa)
    .refine:
      implicit request =>
        request.get[IsIncorporated].getNumberOfCompaniesHouseOfficers match
          case Some(n: SixOrMoreOfficers) => request.add(n)
          case Some(_: FiveOrLessOfficers) =>
            logger.warn("Number of required key individuals is five or less, redirecting to Companies House officers page")
            Redirect(AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url)
          case None => Redirect(AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url)
    .refine:
      implicit request =>
        val agentApplication: IsIncorporated = request.get
        individualProvideDetailsService
          .findAllKeyIndividualsByApplicationId(
            agentApplication.agentApplicationId
          ).map[RequestWithData[DataWithLists] | Result]:
            case Nil => request.add[List[IndividualProvidedDetails]](List.empty[IndividualProvidedDetails])
            case list: List[IndividualProvidedDetails] if list.size <= request.get[SixOrMoreOfficers].totalListSize =>
              request.add[List[IndividualProvidedDetails]](list)
            case _ => Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.CheckYourAnswersController.show.url)
  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val agentApplication: IsIncorporated = request.get[IsIncorporated]
      Ok(view(
        sixOrMoreOfficers = request.get[SixOrMoreOfficers],
        existingList = request.get[List[IndividualProvidedDetails]],
        agentApplication = agentApplication
      ))
