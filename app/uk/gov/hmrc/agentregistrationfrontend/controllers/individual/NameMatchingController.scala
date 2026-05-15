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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.connectors.CitizenDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.forms.individual.IndividualNameSearchForm
import uk.gov.hmrc.agentregistrationfrontend.model.citizendetails.CitizenDetails
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.IndividualNameSearchPage
import uk.gov.hmrc.auth.core.ConfidenceLevel

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class NameMatchingController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  agentApplicationService: AgentApplicationService,
  individualProvideDetailsService: IndividualProvideDetailsService,
  citizenDetailsConnector: CitizenDetailsConnector,
  view: IndividualNameSearchPage
)
extends FrontendController(mcc, actions):

  private type DataWithIndividualProvidedDetailsForSearch =
    Option[CitizenDetails] *: List[IndividualProvidedDetails] *: AgentApplication *: DataWithAdditionalIdentifiers

  def baseAction(
    linkId: LinkId
  ): ActionBuilderWithData[DataWithIndividualProvidedDetailsForSearch] = actions
    .authorisedWithAdditionalIdentifiers
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
          .map: list =>
            /** We filter out any provided details that have an internalUserId that does not match the current user, we keep the current user to support back
              * linking to the search form and submitting again.
              */
            request.add[List[IndividualProvidedDetails]](list.filterNot(individual =>
              individual.internalUserId.isDefined && !individual.internalUserId.contains(request.get[InternalUserId])
            ))
    .refine:
      implicit request =>
        (request.get[ConfidenceLevel], request.get[Option[Nino]]) match
          case (ConfidenceLevel.L250, Some(nino)) =>
            citizenDetailsConnector
              .getCitizenDetails(nino)
              .map[RequestWithData[DataWithIndividualProvidedDetailsForSearch]]: details =>
                request.add[Option[CitizenDetails]](Some(details))
          case _ => request.add[Option[CitizenDetails]](None)

  def show(
    linkId: LinkId
  ): Action[AnyContent] = baseAction(linkId).async:
    implicit request =>
      val unclaimedIndividuals: List[IndividualProvidedDetails] = request.get
      val applicantName = request.get[AgentApplication].getApplicantContactDetails.applicantName.value
      val form = IndividualNameSearchForm.form(unclaimedIndividuals, applicantName)
      Future.successful(Ok(view(
        form = form,
        linkId = linkId,
        applicantName = applicantName
      )))

  def submit(
    linkId: LinkId
  ): Action[AnyContent] = baseAction(linkId).async:
    implicit request =>
      val applicantName = request.get[AgentApplication].getApplicantContactDetails.applicantName.value
      IndividualNameSearchForm.form(request.get[List[IndividualProvidedDetails]], applicantName).bindFromRequest().fold(
        formWithErrors =>
          logger.warn(s"Individual name search form submission had errors: ${formWithErrors.errors}")
          Future.successful(BadRequest(view(
            form = formWithErrors,
            linkId = linkId,
            applicantName = applicantName
          )))
        ,
        matchedIndividual =>
          if
          matchedIndividual.internalUserId.exists(_ === request.get[InternalUserId]) // we already matched before so don't upsert again
          then
            Future.successful(Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url))
          else
            individualProvideDetailsService
              .claimIndividualProvidedDetails(
                individualProvidedDetails = matchedIndividual
                  .copy(passedIv = Some(request.get[ConfidenceLevel] === ConfidenceLevel.L250)),
                internalUserId = request.get[InternalUserId],
                maybeNino = request.get[Option[Nino]],
                citizenDetails = request.get[Option[CitizenDetails]]
              ).map: _ =>
                Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url)
      )
