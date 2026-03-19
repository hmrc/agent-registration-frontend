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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.individual.ConfirmNameMatchForm
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.getIndividualName
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.ConfirmNameMatchPage
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.Credentials

import javax.inject.Inject
import scala.concurrent.Future

class NameMatchConfirmationController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  agentApplicationService: AgentApplicationService,
  view: ConfirmNameMatchPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private def baseAction(
    linkId: LinkId
  ): ActionBuilderWithData[DataWithIndividualProvidedDetails] = actions
    .authorised
    .refine:
      implicit request =>
        agentApplicationService
          .find(linkId)
          .map:
            case Some(agentApplication) => request.add(agentApplication)
            case None => Redirect(AppRoutes.providedetails.ExitController.genericExitPage.url)
    .refine:
      implicit request =>
        individualProvideDetailsService
          .findAllForMatchingWithApplication(request.get[AgentApplication].agentApplicationId)
          .map:
            case list: List[IndividualProvidedDetails] if list.exists(_.internalUserId.contains(request.get[InternalUserId])) =>
              Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url)
            case list: List[IndividualProvidedDetails] =>
              val maybeMatchedIndividual: Option[IndividualProvidedDetails] = list
                .filter(_.internalUserId.isEmpty)
                .find(_.individualName === request.getIndividualName)
              maybeMatchedIndividual match
                case Some(matchedIndividual) => request.add[IndividualProvidedDetails](matchedIndividual)
                case None => Redirect(AppRoutes.providedetails.ContactApplicantController.show.url)

  def show(linkId: LinkId): Action[AnyContent] = baseAction(linkId).async:
    implicit request =>
      Future.successful(Ok(
        view(
          form = ConfirmNameMatchForm.form,
          individualProvidedDetails = request.get[IndividualProvidedDetails],
          linkId = linkId
        )
      ))

  def submit(linkId: LinkId): Action[AnyContent] = baseAction(linkId)
    .ensureValidForm[YesNo](
      form = ConfirmNameMatchForm.form,
      resultToServeWhenFormHasErrors =
        implicit request =>
          formWithErrors =>
            BadRequest(
              view(
                form = formWithErrors,
                individualProvidedDetails = request.get[IndividualProvidedDetails],
                linkId = linkId
              )
            )
    )
    .async:
      implicit request =>
        val userResponse: YesNo = request.get
        userResponse match
          case YesNo.Yes =>
            individualProvideDetailsService
              .claimIndividualNonCiDProvidedDetails(
                individualProvidedDetails = request.get[IndividualProvidedDetails]
                  .copy(passedIv = Some(request.get[ConfidenceLevel] === ConfidenceLevel.L250)),
                internalUserId = request.get[InternalUserId]
              )
            Future.successful(Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url))
          case YesNo.No => Future.successful(Redirect(AppRoutes.providedetails.ContactApplicantController.show.url))
